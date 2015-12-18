package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.reassignment.*;
import com.idiominc.ws.integration.compassion.utilities.rating.*;
import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.metadata.AttributeValidator;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_OBJECT;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_TYPE;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.attribute.WSAttributeValue;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSRole;
import com.idiominc.wssdk.workflow.WSHumanTaskStep;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

//log4j
import org.apache.log4j.Logger;

//java
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Reassign the QC queue step to the list of eligible translators
 */
public class ReassignQCStep  extends WSCustomTaskAutomaticAction {

    //version
    private String version = "1.1";

    //output transition
    private static final String DONE_TRANSITION = "Done";



    //log
    private Logger log = Logger.getLogger(ReassignQCStep.class);

    //regular expression to find user recorded in the attribute
    private final String REGEXP = "\\[(.*?)\\]";
    //attribute that stores the most recent translator
    private final String MOST_RECENT_TRANSLATOR_ATTR = "MostRecentTranslator";
    //various constants
    private static final String _ORIGINAL_LANGUAGE_ATTR = "OriginalLanguage";
    private static final String _TWOSTEP_PREFIX = "[Second Step Project] ";
    private static final String _SDLPROCESS_REQ = "SDLProcessRequired";
    private static final String _TWO_STEP_PROCESS = "TwoStepProcess";
    private static final String _TRANSLATION_PROCESS = "Translation";

    /**
     * Reassign QC Queue Step
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //data
        List<WSUser> resultingList = new ArrayList<WSUser>();
        StringBuilder assignedToSupervisors = new StringBuilder();

        WSHumanTaskStep hts = ReassignStep.getNextHumanTaskStep(task);

        if(null == hts) {
            log.error("Can not locate the very next human step to step " + task.getCurrentTaskStep().getWorkflowStep().getName());
            return new WSActionResult(WSActionResult.ERROR, "Can not locate the very next human step");
        }

        //various metadata
        WSProject project = task.getProject();
        WSWorkgroup workgroup = project.getProjectGroup().getWorkgroup();
        WSLocale sourceLocale = project.getSourceLocale();
        WSLocale targetLocale = project.getTargetLocale();
        String origSrcLocaleStr = project.getAttribute(_ORIGINAL_LANGUAGE_ATTR);

        // Get the source locale from the payload/attribute if this is a manual translation process, only for first-step
        String processRequired = project.getAttribute(_SDLPROCESS_REQ);
        String twoStepProcess = project.getAttribute(_TWO_STEP_PROCESS);
        boolean electronicContent = Boolean.parseBoolean(project.getAttribute("electronicContent"));

        if(twoStepProcess == null || !twoStepProcess.equals(_TWOSTEP_PREFIX)) {
            if(processRequired != null && processRequired.equals(_TRANSLATION_PROCESS) && !electronicContent) {
                sourceLocale = wsContext.getUserManager().getLocale(origSrcLocaleStr);
                if(sourceLocale == null) {
                    return new WSActionResult(WSActionResult.ERROR, "Expected source locale was not found: " + origSrcLocaleStr);
                }
            }
        } else if(sourceLocale.getName().endsWith("-Direct")) {
            // the use of Direct locale as source is only when we perform manual translation into the target
            // Retrieve the source info from the project; this should only be the case of English to <Target> translation
            sourceLocale = wsContext.getUserManager().getLocale(origSrcLocaleStr);
        }

        if(project.getAttribute("LanguageExceptionType") != null && project.getAttribute("LanguageExceptionType").equals("Type3_S2B_NoGP")) {
            // special case handling; must use source locale from the project
            sourceLocale = project.getSourceLocale();
        }


        String translatorRoleStr, supervisorRoleStr;

        try {
            translatorRoleStr = Config.getTranslatorWorkflowRole(wsContext);
            supervisorRoleStr = Config.getSupervisorWorkflowRole(wsContext);
        } catch (Exception e) {
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        }

        WSRole translatorRole = wsContext.getUserManager().getRole(translatorRoleStr);
        WSRole supervisorRole = wsContext.getUserManager().getRole(supervisorRoleStr);

        if(null == translatorRole) {
            return new WSActionResult(WSActionResult.ERROR,  "No workflow role " + translatorRoleStr + " has been found!");
        }

        if(null == supervisorRole) {
            return new WSActionResult(WSActionResult.ERROR,  "No workflow role " + supervisorRoleStr + " has been found!");
        }

        WSAttributeValue complexityValue = project.getAttributeValue(Config._PROJECT_COMPLEXITY_ATTRIBUTE);
        if(null == complexityValue) {
            return new WSActionResult(WSActionResult.ERROR,  "Complexity level is not stored under the project attribute "
                                      + Config._USER_RATING_ATTRIBUTE );
        }
        String complexityStr = complexityValue.getAttributeValue();

        if(null == complexityStr) {
            return new WSActionResult(WSActionResult.ERROR,  "Complexity level is null!");
        }

        try {
            Integer.parseInt(complexityStr);
        } catch (NumberFormatException e) {
            log.error(e);
            return new WSActionResult(WSActionResult.ERROR,  "Complexity level is not numeric " + complexityStr);
        }

        try {
            String qcQualifiedAttributeName;
            WSUser realTranslator = findTranslator(wsContext,
                                                   task.getProject());
            WBAssignmentRuleIdentifier rid = new WBAssignmentRuleIdentifier(wsContext, workgroup);
            List<RATING> acceptableRatings = rid.getListOfRatingsPerComplexity(RATING_APPLICABILITY.QC,
                                            complexityStr);
            try {
               //obtain the names of the attributes
               qcQualifiedAttributeName = Config.getQCQualifiedAttributeName(wsContext);
               if(null != realTranslator) {
                       AttributeValidator.validateAttribute(wsContext,
                                                 qcQualifiedAttributeName,
                                                 ATTRIBUTE_OBJECT.USER,
                                                 ATTRIBUTE_TYPE.BOOLEAN,
                                                 realTranslator,
                                                 null);
               }
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
                return new WSActionResult(WSActionResult.ERROR,
                        "Attributes Misconfiguration. " + e.getLocalizedMessage());
            }

            for(WSUser candidate: wsContext.getUserManager().getUsers()) {
             boolean isUserQualified = WSAttributeUtils.getBooleanAttribute(this, candidate, qcQualifiedAttributeName);
             if(!isUserQualified) {
                 log.info("User " + candidate.getUserName() + " is disqualified for QC as defined in User setting");
                 continue;
             }
             if(realTranslator != null
                &&
                realTranslator.getId() == candidate.getId()) {
                 //do not assign QC step to a translator who did original translaiton
                 log.info("User " + candidate.getUserName() + " is disqualified for QC. Did translation");
                 continue;
             }
             if(ReassignStep.belongs(candidate.getId(), targetLocale)
                &&
                ReassignStep.belongs(candidate.getId(), sourceLocale)
                &&
                ReassignStep.belongs(candidate.getId(), translatorRole)
                &&
                ReassignStep.belongs(candidate.getId(), workgroup)) {

                 try {
                    UserRatingParser parser = new UserRatingParser(candidate);
                    RATING userRating = parser.getRating(wsContext, sourceLocale, targetLocale);
                    if(acceptableRatings.contains(userRating)) {
                      resultingList.add(candidate);
                    }
                 }catch (RatingException e) {
                     //ignore this exception, user is just not qualified for this assignment
                     log.info(e);
                 }
             }
          }
        } catch(RatingException e) {
             log.error(e.getMessage(), e);
             return new WSActionResult(WSActionResult.ERROR,  e.getLocalizedMessage());
        }

        if(0 == resultingList.size()) {
            log.info("Opting out to supervisor's role users");
            assignedToSupervisors.append("Assigned QC Queue step to Supervisors - no qualified Translator has been found.");
            WSUser realTranslator = findTranslator(wsContext,
                                                   task.getProject());
            for(WSUser candidate: supervisorRole.getUsers()) {
                if(realTranslator != null
                   &&
                   realTranslator.getId() == candidate.getId()) {
                    //do not assign the step to supervisor if one did the translation
                    log.info("User " + candidate.getUserName() + " is disqualified for QC. Did translation");
                    continue;
                }
               if(ReassignStep.belongs(candidate.getId(), targetLocale)
                  &&
                  ReassignStep.belongs(candidate.getId(), sourceLocale)
                  &&
                  ReassignStep.belongs(candidate.getId(), workgroup))
                  {
                      resultingList.add(candidate);
                  }
            }
        }

        if(0 == resultingList.size()) {
            log.error("Not qualified users found - on both Translator and Supervisor roles!");
            return new WSActionResult(WSActionResult.ERROR,  "Not qualified users found - in both Translator and Supervisor roles!");
        }

        ReassignStep.reassign(hts, resultingList.toArray(new WSUser[resultingList.size()]));

        return new WSActionResult(getReturns()[0], "Reassigned step " +
                                                   hts.getWorkflowStep().getName() +
                                                   " to " + resultingList.size() + " qualified users. " + assignedToSupervisors.toString());
    }

    /**
     * Locate the most recent translator on the project
     * @param context - WS Context
     * @param project - current project
     * @return  the most recent translator or NULL if not found
     */
    private WSUser findTranslator(WSContext context,
                                  WSProject project)  {
        try {
            AttributeValidator.validateAttribute(context,
                                                 MOST_RECENT_TRANSLATOR_ATTR,
                                                 ATTRIBUTE_OBJECT.PROJECT,
                                                 ATTRIBUTE_TYPE.TEXT,
                                                 project,
                                                 "");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return null;
        }

        Pattern p = Pattern.compile(REGEXP);
        String attributeValue =  project.getAttribute(MOST_RECENT_TRANSLATOR_ATTR);
        if(null == attributeValue || 0 == attributeValue.length()) {
            log.error("Translator was not recorded in " + MOST_RECENT_TRANSLATOR_ATTR + " attribute");
            return null;
        }
        WSUser translator;
        Matcher m = p.matcher(attributeValue);
        if(m.find()) {
            String userName = m.group(1);
            translator = context.getUserManager().getUser(userName);
            if(null == translator) {
                log.error("Can not locate user whose userName is " + userName);
            }
            return translator;
        } else {
            log.error("Can not parse user info from " + attributeValue);
        }
        return null;
    }

    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {DONE_TRANSITION};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Reassign QC Queue Step";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Reassign QC Queue Step to eligible translators based on " +
                " workflow role, workgroup, language, and translation complexity rating";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }
    
}
