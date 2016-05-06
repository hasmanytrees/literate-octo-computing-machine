package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.rating.*;
import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.qc.QCSamplingRate;
import com.idiominc.ws.integration.compassion.utilities.metadata.*;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticActionWithParameters;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.attribute.WSAttributeValue;
import com.idiominc.wssdk.attribute.WSSelectStringAttributeValue;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;

//log4j
import org.apache.log4j.Logger;

//java
import java.util.Map;

/**
 * Make determination if QC stage is required for a task via QC sampling requirements.
 * QC Sampling is based on translator rating, sample rate, and XML payload attribute.
 *
 * @author SDL Professional Services
 */
public class DetermineQCSampling extends WSCustomTaskAutomaticActionWithParameters {

    //version
    private String version = "1.1";

    //output transitions
    private static final String SKIP_QC = "Skip QC";
    private static final String QC_REQUIRED = "QC Required";

    //log
    private Logger log = Logger.getLogger(DetermineQCSampling.class);

    //constants
    private static final String _TRANSLATION_REJECTED = "Fail";
    private static final String _TWOSTEP_PREFIX = "[Second Step Project] ";
    private static final String _SDLPROCESS_REQ = "SDLProcessRequired";
    private static final String _TWO_STEP_PROCESS = "TwoStepProcess";
    private static final String _TRANSLATION_PROCESS = "Translation";
    private static final String _ORIGINAL_LANGUAGE_ATTR = "OriginalLanguage";
    private static final String _DIRECTION_ATTR = "Direction";

    //parameters
    private final static String _CHECKSTOPWORDS = "CHECKSTOPWORDS";
    private boolean checkStopWords;

    /**
     * Creates parameters for this AA
     * @return  Array of parameters
     */
    public WSParameter[] getParameters() {
        return new WSParameter[]{
            WSParameterFactory.createBooleanParameter(_CHECKSTOPWORDS, "Check Stop Words?", true)
        };
    }

    /**
    * Load AA parameters into the variables
    * @param parameters - map of the parameters
    * @throws com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException - exepction
    */
   public void preLoadParameters(Map parameters) throws WSInvalidParameterException {
       checkStopWords = (WSParameterFactory.BOOLEAN_TRUE_VALUE.equals(preLoadParameter(parameters, _CHECKSTOPWORDS, true)));
   }


    /**
     *  Determine if QC stage is required
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //various metadata
        WSProject project = task.getProject();
        WSLocale sourceLocale = project.getSourceLocale();
        WSLocale targetLocale = project.getTargetLocale();
        WSUser translator = task.getTaskHistory().getLastHumanStepUser();

        // check the QC check override flag first
        String qcNotRequiredOverride = task.getProject().getAttribute("qcNotRequiredOverride");
        if(qcNotRequiredOverride != null && qcNotRequiredOverride.equals("true")) {
            task.getProject().setAttribute("qcNotRequiredOverride", "false");
            // qc not required was overriden; take this value and return this one time
            return new WSActionResult(SKIP_QC, "QC is not required.");
        }

        // Get the letter direction
        String letterDirection = project.getAttribute(_DIRECTION_ATTR);

        // Get the source locale from the payload/attribute if this is a manual translation process, only for first-step
        String processRequired = project.getAttribute(_SDLPROCESS_REQ);
        String twoStepProcess = project.getAttribute(_TWO_STEP_PROCESS);
        if(twoStepProcess == null || !twoStepProcess.equals(_TWOSTEP_PREFIX)) {
            if(processRequired != null && processRequired.equals(_TRANSLATION_PROCESS)) {
                String origSrcLocaleStr = project.getAttribute(_ORIGINAL_LANGUAGE_ATTR);
                sourceLocale = wsContext.getUserManager().getLocale(origSrcLocaleStr);
                if(sourceLocale == null) {
                    return new WSActionResult(WSActionResult.ERROR, "Expected source locale was not found: " + origSrcLocaleStr);
                }
            }
        }

        if(null == translator) {
            return new WSActionResult(WSActionResult.ERROR, "Can not determine asset's translator");
        }

        log.info("Translator is " + translator.getFullName());

        //obtain and validate all attributes
        String attrTCount; //translation count
        String attrTranslationRejected; //was translation rejected? The name of this attribute is qcResult
                                        //with valid outputs: Fail, Pass
        String attrStopWordsList; //stop words list. The name of this attribute is stopwordAttr
                                  //and this attribute is a large text area
//        String attrMandatoryReview = Enumeration_Attributes.MandatoryReview.getAttributeName(); //is review required?
        String attrMandatoryReview = null;

        try {

            //obtain the names of the attributes
            attrTCount = Config.getTranslationsCountAttributeName(wsContext);
            attrTranslationRejected = Config.getTranslationRejectedAttributeName(wsContext);
            attrStopWordsList = Config.getStopWordsAttributeName(wsContext);

            //check if attributes were properly configured in the first place
            AttributeValidator.validateAttribute(
                    wsContext,
                    attrTCount,
                    ATTRIBUTE_OBJECT.USER,
                    ATTRIBUTE_TYPE.INTEGER,
                    translator,
                    "0");

            CIMetadataInfo mandatoryReview = CIMetadataConfig.getMetadataItem(letterDirection,"MandatoryReview");

            if(mandatoryReview != null) {

                attrMandatoryReview = mandatoryReview.getAttributeName();
                AttributeValidator.validateAttribute(
                        wsContext,
                        attrMandatoryReview,
                        mandatoryReview.getAttributeObject(),
                        mandatoryReview.getAttributeType(),
                        project,
                        "true");
            }

            AttributeValidator.validateAttribute(
                    wsContext,
                    attrTranslationRejected,
                    ATTRIBUTE_OBJECT.TASK,
                    ATTRIBUTE_TYPE.SELECTOR,
                    task,
                    "");

            AttributeValidator.validateAttribute(
                    wsContext,
                    attrStopWordsList,
                    ATTRIBUTE_OBJECT.TASK,
                    ATTRIBUTE_TYPE.TEXT,
                    task,
                    "");

        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                    "Attributes Misconfiguration. " + e.getLocalizedMessage());
        }


        //obtain translator's rating
        RATING userRating;
        try {
           UserRatingParser parser = new UserRatingParser(translator, null);
           userRating = parser.getRating(wsContext, sourceLocale, targetLocale);
        }catch (RatingException e) {
            // user was not qualified for this assignment, so assume this is a beginner
            log.warn("Can't figure out the rating of the translator!");
            userRating = RATING.BEGINNER;
        }

        //obtain global sampling rate
        String samplingRateStr;
        try {
            samplingRateStr = QCSamplingRate.getSampleRate(wsContext, userRating.toString());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                                      "Can not obtain global samoling rate for " + userRating.toString());

        }

        //error-out if sampling rate is not defined
        if(null == samplingRateStr) {
            return new WSActionResult(WSActionResult.ERROR,
                                      "Sampling rate for " + userRating.toString() + " is not defined!");
        }

        int samplingRate;
        try {
            samplingRate = Integer.parseInt(samplingRateStr);
        } catch (Exception e) {
            log.error("Sampling rate for " + userRating.toString() + " is not numeric: " + samplingRateStr);
            return new WSActionResult(WSActionResult.ERROR,
                                      "Sampling rate for " + userRating.toString() + " is not numeric: " + samplingRateStr);
        }

        if(samplingRate <= 0) {
            log.error("Sampling rate for " + userRating.toString() + " is less than 1: " + samplingRate);
            return new WSActionResult(WSActionResult.ERROR,
                                      "Sampling rate for " + userRating.toString() + " is less than 1: " + samplingRate);

        }

        log.info("Sampling rate for " + userRating.toString() + " is " + samplingRate);

        int tasksTranslated = WSAttributeUtils.getIntegerAttribute(this,
                                                                   translator,
                                                                   attrTCount);
        if(tasksTranslated <= 0) {
            return new WSActionResult(WSActionResult.ERROR,
                                      "Invalid number of translated letters for user " +
                                       translator.getUserName() +
                                       ": " + tasksTranslated);
        }

        log.info("Translator " + translator.getUserName() +
                 " has translated " + tasksTranslated + " letters.");

        if(tasksTranslated % samplingRate == 0) {
            return new WSActionResult(QC_REQUIRED,
                                      "QC is required by sampling rule: Translated " +
                                      tasksTranslated + "; sampling rate: " + samplingRate);
        }

        // MandatoryReview attribute does not always exist in all letter types
        if(attrMandatoryReview != null) {
            if (WSAttributeUtils.getBooleanAttribute(this,
                    project,
                    attrMandatoryReview)) {
                return new WSActionResult(QC_REQUIRED,
                        "QC is required. Mandatory Review Flag is set.");
            }
        }

        if(checkStopWords) {
          try {
             String stopWords = WSAttributeUtils.getStringValue(wsContext,
                                task.getAttributeValue(attrStopWordsList));
              if(stopWords != null && stopWords.length() > 0) {
                  return new WSActionResult(QC_REQUIRED,
                                            "QC is required. Found at least one stop word.");
              }
          } catch(Exception e) {
           log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                                      e.getLocalizedMessage());
          }
        }

        WSAttributeValue status = task.getAttributeValue(attrTranslationRejected);
        if(!(status instanceof WSSelectStringAttributeValue)) {
            return new WSActionResult(WSActionResult.ERROR,
                                      "Misconfigured attribute - " + attrTranslationRejected);
        }
        String statusValue = ((WSSelectStringAttributeValue)status).getValue();
        if(_TRANSLATION_REJECTED.equalsIgnoreCase(statusValue)) {
            return new WSActionResult(QC_REQUIRED,
                                      "QC is required. Translation has been rejected");
        }

        //all tests results have been negative, so QC is not required
        return new WSActionResult(SKIP_QC, "QC is not required.");
    }


    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {SKIP_QC, QC_REQUIRED};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Is QC Required?";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Determine if the QC should take place for the given task " +
                "based on the translator rating, task history, and other considerations.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
