package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.reassignment.*;
import com.idiominc.ws.integration.compassion.utilities.rating.*;
import com.idiominc.external.config.Config;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;

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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//java
import java.util.List;
import java.util.ArrayList;

/**
 * Reassign the translate queue step to the list of eligible translators
 */
public class ReassignQueueStep extends WSCustomTaskAutomaticAction {

    //version
    private String version = "1.1";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    //log
    private Logger log = Logger.getLogger(ReassignQueueStep.class);

    //variables
    private final static String _ORIGINAL_LANGUAGE_ATTR = "OriginalLanguage";
    private static final String _TWOSTEP_PREFIX = "[Second Step Project] ";
    private static final String _SDLPROCESS_REQ = "SDLProcessRequired";
    private static final String _TWO_STEP_PROCESS = "TwoStepProcess";
    private static final String _TRANSLATION_PROCESS = "Translation";

    /**
     * Assign translation metadatra to projects and tasks
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {
//        log.setLevel(Level.INFO);

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

        //for first-step project, get source locale info from the XML payload
        String processRequired = project.getAttribute(_SDLPROCESS_REQ);
        String twoStepProcess = project.getAttribute(_TWO_STEP_PROCESS);
        boolean electronicContent = Boolean.parseBoolean(project.getAttribute("electronicContent"));

        if(twoStepProcess == null || !twoStepProcess.equals(_TWOSTEP_PREFIX)) {
            // If part of first-step process, then check the source locale from the payload for 'manual translation process'
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
            WBAssignmentRuleIdentifier rid = new WBAssignmentRuleIdentifier(wsContext, workgroup);
            List<RATING> acceptableRatings = rid.getListOfRatingsPerComplexity(RATING_APPLICABILITY.TRANSLATE,
                                                                               complexityStr);

          for(WSUser candidate: wsContext.getUserManager().getUsers()) {
              if(ReassignStep.belongs(candidate.getId(), targetLocale)
                      &&
//                      ReassignStep.belongs(candidate.getId(), sourceLocale) // Necessary to check source locale membership?
//                      &&
                      ReassignStep.belongs(candidate.getId(), translatorRole)
                      &&
                      ReassignStep.belongs(candidate.getId(), workgroup)) {

                  try {
                      // expect RatingException, if user is not set with rating data; should not break the customization
                      log.info("Testing candidacy for " + candidate.getUserName() + " for " + sourceLocale.getName() + "-->" + targetLocale.getName());
                      UserRatingParser parser = new UserRatingParser(candidate);
                      RATING userRating = parser.getRating(wsContext, sourceLocale, targetLocale);
                      if(acceptableRatings.contains(userRating)) {
                          resultingList.add(candidate);
                      }
                  }catch (RatingException e) {
                      //ignore this, user is just not qualified for this assignment
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
            assignedToSupervisors.append("Assigned Translator Queue step to Supervisors - no qualified Translator found.");
            for(WSUser candidate: supervisorRole.getUsers()) {
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

        //ReassignStep.reassign(hts, new WSUser[] {executor});
        return new WSActionResult(getReturns()[0], "Reassigned step " +
                                                   hts.getWorkflowStep().getName() +
                                                   " to " + resultingList.size() + " qualified users. " + assignedToSupervisors.toString());
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
        return "Reassign Translation Queue Step";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Reassign Translation Queue Step to eligible translators based on " +
                " workflow role, workgroup, language, and translation complexity rating";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
