package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.reassignment.*;
import com.idiominc.ws.integration.compassion.utilities.metadata.AttributeValidator;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_OBJECT;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_TYPE;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.user.WSRole;
import com.idiominc.wssdk.workflow.WSHumanTaskStep;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

//Compassion
import com.idiominc.external.config.Config;

//log4j
import org.apache.log4j.Logger;

//java
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

/**
 * An automatic action to identify and assign next human step to most recent translator
 *
 * @author SDL Professional Services
 */
public class ReassignNextStepToTranslator extends WSCustomTaskAutomaticAction {

    //version
    private String version = "1.0";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    //regular expression to find user recorded in the attribute
    private final String REGEXP = "\\[(.*?)\\]";

    //attribute that stores the most recent translator
    private final String MOST_RECENT_TRANSLATOR_ATTR = "MostRecentTranslator";
    //log
    private Logger log = Logger.getLogger(ReassignNextStepToTranslator.class);


    /**
     * Assign translation metadatra to projects and tasks
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {
        WSHumanTaskStep hts = ReassignStep.getNextHumanTaskStep(task);
        if(null == hts) {
            log.error("Can not locate the very next human step to step " + task.getCurrentTaskStep().getWorkflowStep().getName());
            return new WSActionResult(WSActionResult.ERROR, "Can not locate the very next human step");
        }

        String supervisorRoleStr;

        try {
            supervisorRoleStr = Config.getSupervisorWorkflowRole(wsContext);
        } catch (Exception e) {
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        }

        WSRole supervisorRole = wsContext.getUserManager().getRole(supervisorRoleStr);
        if(null == supervisorRole) {
            return new WSActionResult(WSActionResult.ERROR,  "No workflow role " + supervisorRoleStr + " has been found!");
        }
        try {
            AttributeValidator.validateAttribute(wsContext,
                                                 MOST_RECENT_TRANSLATOR_ATTR,
                                                 ATTRIBUTE_OBJECT.PROJECT,
                                                 ATTRIBUTE_TYPE.TEXT,
                                                 task.getProject(),
                                                 "");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                                      "Attribute " + MOST_RECENT_TRANSLATOR_ATTR + " is misconfigured. " + e.getLocalizedMessage());
        }

        Pattern p = Pattern.compile(REGEXP);
        String attributeValue =  task.getProject().getAttribute(MOST_RECENT_TRANSLATOR_ATTR);
        if(null == attributeValue || 0 == attributeValue.length()) {
            log.error("Can't reassig step " +
                                                   hts.getWorkflowStep().getName() +
                                                   " to translator as nothing is recorded in project attribute " + MOST_RECENT_TRANSLATOR_ATTR);
            return reassignToSupervisors(hts,
                                        supervisorRole,
                                        task.getProject().getTargetLocale(),
                                        task.getProject().getProjectGroup().getWorkgroup());
        }
        WSUser translator;
        Matcher m = p.matcher(attributeValue);
        if(m.find()) {
            String userName = m.group(1);
            translator = wsContext.getUserManager().getUser(userName);
            if(null == translator) {
                log.error("Can not locate user whose userName is " + userName);
                return reassignToSupervisors(hts,
                                            supervisorRole,
                                            task.getProject().getTargetLocale(),
                                            task.getProject().getProjectGroup().getWorkgroup());
            }
            ReassignStep.reassign(hts, new WSUser[] {translator});
        } else {
            log.error("Can not parse user info from " + attributeValue);
            return reassignToSupervisors(hts,
                                        supervisorRole,
                                        task.getProject().getTargetLocale(),
                                        task.getProject().getProjectGroup().getWorkgroup());
        }
        return new WSActionResult(getReturns()[0], "Reassigned step " +
                                                   hts.getWorkflowStep().getName() +
                                                   " to " +  translator.getFullName()
                                                   + " (" + translator.getUserName() + ")");
    }

    /**
     * Reassign step to eligible supervisors
     * @param hts - human task step
     * @param supervisorRole - supervisor Role as configured in WS
     * @param targetLocale - project's target locale
     * @param workgroup - project's workgroup
     * @return OK transition out of the automatic action should we have at least 1 eligible supervisor. ERROR if we find none.
     */
    private WSActionResult reassignToSupervisors(WSHumanTaskStep hts,
                                                 WSRole supervisorRole,
                                                 WSLocale targetLocale,
                                                 WSWorkgroup workgroup) {
        List<WSUser> supervisors = new ArrayList<WSUser>();
        for(WSUser candidate: supervisorRole.getUsers()) {
               if(ReassignStep.belongs(candidate.getId(), targetLocale)
                  &&
                  ReassignStep.belongs(candidate.getId(), workgroup))
                  {
                      supervisors.add(candidate);
                  }
        }

        if(0 == supervisors.size()) {
            return new WSActionResult(WSActionResult.ERROR,"No qualified supervisors found!");
        } else {
            ReassignStep.reassign(hts, supervisors.toArray(new WSUser[supervisors.size()]));
            return new WSActionResult(getReturns()[0], "Reassigned step " +
                                                       hts.getWorkflowStep().getName() +
                                                       " to " + supervisors.size() + " sepervisors.");
        }

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
        return "Reassign Next Step to Translator";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Reassign the next human step in a workflow to the most recent executor of the Translate human step as recorded in a project attribute";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
