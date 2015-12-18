package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.metadata.*;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

//log4j
import org.apache.log4j.Logger;

/**
 * Increments count of letters the translator has been working on
 */
public class IncrementTranslationCount extends WSCustomTaskAutomaticAction{

    //version
    private String version = "1.0";

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(IncrementTranslationCount.class);

    //variable
    private static final String _mostRecentTranslatorAttr = "MostRecentTranslator";

    /**
     * Increment count of letters the translator has been working on
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //various metadata
        WSUser translator = task.getTaskHistory().getLastHumanStepUser();

        if(null == translator) {
            return new WSActionResult(WSActionResult.ERROR, "Can not determine asset's translator");
        }
        log.info("Translator is " + translator.getFullName());


        //set the translator attribute for later use, for FO only
//        String wkgroupName = task.getProject().getProjectGroup().getWorkgroup().getName();
//        if(wkgroupName.startsWith("FO_")) {
            task.getProject().setAttribute(_mostRecentTranslatorAttr, translator.getFirstName() + " " + translator.getLastName()
                    + " [" + translator.getUserName() + "]");
//        }

        //obtain count attribute
        String attrTCount = null;
        try {
            attrTCount = Config.getTranslationsCountAttributeName(wsContext);
            AttributeValidator.validateAttribute(wsContext,
                                                 attrTCount,
                                                 ATTRIBUTE_OBJECT.USER,
                                                 ATTRIBUTE_TYPE.INTEGER,
                                                 translator,
                                                 "0");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                                      "Attribute " + attrTCount + " is misconfigured. " + e.getLocalizedMessage());
        }


        int tasksTranslated;
        if(null == translator.getAttribute(attrTCount)
           || translator.getAttribute(attrTCount).length() == 0) {
              log.info("First time user. Set initial count of translated letters to 0");
              tasksTranslated = 0;
        }
        else {
            tasksTranslated = WSAttributeUtils.getIntegerAttribute(this,
                                                                   translator,
                                                                   attrTCount);
        }

        if(tasksTranslated < 0) {
            return new WSActionResult(WSActionResult.ERROR,
                                      "Invalid number of translated letters for user " +
                                       translator.getUserName() +
                                       ": " + tasksTranslated);
        }

        translator.setAttribute(attrTCount,
                                String.valueOf(++tasksTranslated));

        return new WSActionResult(DONE,
                                  "User " + translator.getFullName() + " ("
                                  + translator.getUserName() + ") has translated "
                                  + tasksTranslated + " assets");

    }


    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {DONE};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Account for Translation";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records information that user has translated this asset";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
