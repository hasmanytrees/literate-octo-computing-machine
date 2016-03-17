package com.idiominc.ws.integration.compassion.rules.monitor.action;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.rules.nocondition.Generic;
import com.idiominc.ws.integration.compassion.utilities.reassignment.ReassignStep;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.rule.WSActionClauseComponent;
import com.idiominc.wssdk.component.rule.WSActionClauseResults;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptor;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptorFactory;
import com.idiominc.wssdk.component.rule.parameter.WSIntegerClauseParameterValue;
import com.idiominc.wssdk.component.rule.parameter.WSStringClauseParameterValue;
import com.idiominc.wssdk.rule.WSRule;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.workflow.*;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * WorldServer Business Rule to check for claimed tasks longer than input-parameter and automatically unclaim.
 *
 * Iterate through all active tasks in the system and identify those that are at Translate/QC step.
 * For Translate/QC steps, confirm if the tasks have been at the Translate/QC/Update steps for longer than expected.
 * If longer than the input-parameter, "unclaim" by pushing it back to the corresponding Queue steps.
 *
 * [March, 2016] Enhancements added to support the dynamic reassignment where the rule will re-evaluate the step assignment.
 * This change will support the scenario where projects are already created in WorldServer, and then new users are added to the system.
 * Traditionally these users are not assigned to previously allocated step.
 * This rule will compare the current list of assignees and update it if new assignees are found.
 *
 * @author SDL Professional Services
 */
public class CIBusinessRule extends WSActionClauseComponent {

    //log
    private Logger log = Logger.getLogger(AutoUnclaimRule.class);

    // rule parameters
    public static String PARAM_CLAIMED_DURATION_HOURS   = "claimedDurationHours";
    public static String PARAM_CLAIMED_DURATION_MINUTES = "claimedDurationMinutes";
    public static String PARAM_CLAIMED_STEPS            = "claimedSteps";
    public static String PARAM_REASSIGN_STEPS           = "reassignSteps";

    // Rule default step names
    public static final String STEP_TRANSLATION_QUEUE = "Translation Queue";
    public static final String STEP_QC_QUEUE = "QC Queue";

    /*
     * Execute the rule; identify the given parameters, and parse through the list of active tasks in the system,
     * and identify tasks that have been in "Accepted/Claimed" state for longer than expected, and put them
     * back into appropriate Queue steps.
     *
     * @param context - WorldServer Context
     * @param rule - WorldServer Rule for this given rule action clause
     * @param objects - WorldServer objects against which the rule is being executed
     * @param parameters - List of parameters included with the rule definition
     */
    public WSActionClauseResults execute(WSContext context, WSRule rule, Set objects, Map parameters) {

        // Build output message for rule execution log
        StringBuilder msg = new StringBuilder();

        // retrieve CSV-formatted step names parameter
        String stepNamesParamStr = ((WSStringClauseParameterValue) parameters.get(
                autoUnclaimStepsParam.getName())).getValue();
        String[] allStepNamesParam;
        if(stepNamesParamStr.contains(",")) {
            allStepNamesParam = stepNamesParamStr.split(",");
        } else {
            allStepNamesParam = new String[] {stepNamesParamStr};
        }

        // retrieve CSV-formatted reassign step names parameter
        String reassignStepNamesParamStr = ((WSStringClauseParameterValue) parameters.get(
                autoReassignStepsParam.getName())).getValue();
        String[] allReassignStepNamesParam;
        if(reassignStepNamesParamStr.contains(",")) {
            allReassignStepNamesParam = reassignStepNamesParamStr.split(",");
        } else {
            allReassignStepNamesParam = new String[] {reassignStepNamesParamStr};
        }

        // retrieve allowed claimed time duration (hours first, then minutes) parameter
        Integer autoUnclaimDurationHoursParamValue = ((WSIntegerClauseParameterValue) parameters.get(
                autoUnclaimDurationHoursParam.getName())).getValue();

        if(autoUnclaimDurationHoursParamValue < 0) {
            log.error("Invalid configuration for duration(hours) - defaulting to 8 hours");
            autoUnclaimDurationHoursParamValue = 8;
        }

        Integer autoUnclaimDurationMinutesParamValue = ((WSIntegerClauseParameterValue) parameters.get(
                autoUnclaimDurationMinutesParam.getName())).getValue();

        if(autoUnclaimDurationMinutesParamValue < 0 || autoUnclaimDurationMinutesParamValue > 59) {
            log.error("Invalid configuration for duration(minutes)");
            autoUnclaimDurationMinutesParamValue = 0;
        }

        log.debug("********** Begin Processing all Active Tasks for Auto-Unclaim Rule *********");

        WSProject[] activeProjects = context.getWorkflowManager().getProjects(
                new WSProjectStatus[]{WSProjectStatus.ACTIVE});

        try {
            // Iterate through all active projects in the system, and identify tasks that need to be pushed back to Queue
            for(WSProject project : activeProjects) {
                // Iterate through all active tasks in the system, for each active projects
                for (WSTask task : project.getActiveTasks()) {
                    WSAssetTask assetTask = (WSAssetTask) task;
                    String currentStepName = assetTask.getCurrentTaskStep().getWorkflowStep().getName();

                    // Perform the claimant check only for given specified workflow step
                    if(currentStepName != null && stepFound(currentStepName, allStepNamesParam)) {

                        WSUser assignedUser = ((WSHumanTaskStep)assetTask.getCurrentTaskStep()).getUserAssignees()[0];
                        if(assignedUser == null) {
                            // we expect the user to be assigned! Cannot perform auto-unclaim for unassigned tasks!
                            String errMsg = "Current step must be assigned to a specific user for auto-unclaim to work! [" +
                                    assetTask.getSourcePath() +
                                    "]";
                            log.error(errMsg);
                            msg.append(errMsg).append("<br>");

                            continue;
                        }

                        // Found the accepted task step! identify last queue step for given task step
                        WSTaskStep lastQueueStep = findLastQueueStep(context, assetTask, currentStepName);
                        if(lastQueueStep == null) {
                            // Could not find last queue step; unexpected! Log and continue with next task!
                            String errMsg = "Could not identify last queue step for task: " + assetTask.getSourcePath();
                            log.error(errMsg);
                            msg.append(errMsg).append("<br>");
                            continue;
                        }

                        // Get the completion of last Queue step, to determine if push-back to Queue will be required
                        Date lastQueueStepCompletionDate = lastQueueStep.getCompletionDate();

                        // Evaluate if "unclaim" is needed based on most recent Queue completion event
                        if(evaluateForUnclaim(lastQueueStepCompletionDate,
                                autoUnclaimDurationHoursParamValue,
                                autoUnclaimDurationMinutesParamValue)) {
                            // This has been claimed for longer than the parameter; unclaim and push back to Queue!
                            assetTask.completeCurrentTaskStep("Return to Queue", "Auto-unclaimed and returned " +
                                    "to the Queue as it was claimed for longer than the expected duration!");

                            msg.append("Task ").append(assetTask.getSourcePath()).
                                    append(" at step: ").append(currentStepName).
                                    append(" was automatically unclaimed from ").append(assignedUser.getUserName()).
                                    append("!<br>\n");
                        }
                    }
                    // Perform the reassign check only for given specified workflow step
                    // [Enhancement March, 2016] This portion handles dynamic reassignment with shared reassignment logic
                    else if(currentStepName != null && stepFound(currentStepName, allReassignStepNamesParam)) {
                        StringBuilder returnMsg = new StringBuilder();
                        boolean success = false;
                        WSHumanTaskStep currentStep = (WSHumanTaskStep)assetTask.getCurrentTaskStep();
                        if(currentStepName.equals(STEP_TRANSLATION_QUEUE)) {
                            success = ReassignStep.reassignTranslationQueue(context, assetTask, log, returnMsg, currentStep);
                        } else if(currentStepName.equals(STEP_QC_QUEUE)) {
                            success = ReassignStep.reassignQCQueue(context, assetTask, log, returnMsg, this, currentStep);
                        }

                        if(success) {
                            if(!returnMsg.toString().isEmpty()) {
                                msg.append("Project[").append(project.getId()).append("] reassigned:").append(returnMsg.toString()).append("<br><br>");
                            }
                            // otherwise nothing is needed; only output message for actual reassignment
                        } else {
                            msg.append("Project[").append(project.getId()).append("] was not reassigned:").append(returnMsg.toString()).append("<br><br>");
                        }
                    }
                    // otherwise the current step does not match the target step to check for claimant or reassign; continue with rest of tasks
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            msg.append(e.getMessage());
        }


        return new WSActionClauseResults("Done. <br>\n" + msg.toString());
    }

    /**
     * Identify last "Queue" human workflow step for given human step.
     * Translate step is preceded by Translation Queue.
     * Perform QC step is preceded by QC Queue.
     * Update Translation step is preceded by Update Translation Queue.
     *
     * @param context WorldServer context
     * @param assetTask WorldServer asset-based task that is being reviewed for last queue step
     * @param currentStepName current human workflow step, either Translate, Perform QC or Update Translation
     * @return Previous Queue workflow step if found. Null if not found.
     */
    private WSTaskStep findLastQueueStep(WSContext context,
                                         WSAssetTask assetTask,
                                         String currentStepName) {

        String targetQueueStepName;
        try {
            if(currentStepName.equals(Config.getTranslateStepName(context))) {
                targetQueueStepName = Config.getTranslationQueueStepName(context);
            } else if(currentStepName.equals(Config.getQCStepName(context))) {
                targetQueueStepName = Config.getQCQueueStepName(context);
            } else if(currentStepName.equals(Config.getUpdateTranslationStepName(context))) {
                targetQueueStepName = Config.getUpdateTranslationQueueStepName(context);
            } else {
                // could not identify the queue step
                return null;
            }
        } catch(Exception e) {
            return null;
        }

        WSTaskStep[] arrQualifiedSteps = assetTask.getSteps(targetQueueStepName);
        if(null == arrQualifiedSteps || 0 == arrQualifiedSteps.length) {
            return null;
        }

        WSTaskStep lastStep = arrQualifiedSteps[0];
        for(int i = 1; i < arrQualifiedSteps.length; i++) {
            if(lastStep.getCompletionDate() != null && arrQualifiedSteps[i].getCompletionDate() != null) {
                if(lastStep.getCompletionDate().getTime() < arrQualifiedSteps[i].getCompletionDate().getTime()) {
                    lastStep = arrQualifiedSteps[i];
                }
            }
        }

        if(lastStep == null) {
            return null;
        } else {
            return lastStep;
        }

    }

    /**
     * Evaluate for unclaim given current task that is at identified step,
     * by comparing the last Queue completion timestamp and allowed duration
     * @param lastQueueStepCompletionDate Last Queue step completion timestamp
     * @param autoUnclaimDurationHoursParam Allowed Hours duration
     * @param autoUnclaimDurationMinutesParam Allowed Minutes duration
     * @return true if current task has been at the step for longer than allowed duration; false otherwise.
     */
    private boolean evaluateForUnclaim(Date lastQueueStepCompletionDate,
                                       Integer autoUnclaimDurationHoursParam,
                                       Integer autoUnclaimDurationMinutesParam) {

        // calculate the time difference to identify if unclaim is needed.
        Calendar lscd = Calendar.getInstance();
        lscd.setTime(lastQueueStepCompletionDate);
        lscd.add(Calendar.HOUR, autoUnclaimDurationHoursParam);
        lscd.add(Calendar.MINUTE, autoUnclaimDurationMinutesParam);
        Calendar now = Calendar.getInstance();
        return now.after(lscd);
    }

    /**
     * Identify the step to check for unclaim rule from the step names parameter input
     * @param currentStepName current workflow step name
     * @param allStepNamesStr all workflow step names to check for auto-unclaim from parameter
     * @return true if current step needs to be checked for auto-unclaim; false otherwise.
     */
    private boolean stepFound(String currentStepName, String[] allStepNamesStr) {

        for (String stepName : allStepNamesStr) {
            if(stepName.equalsIgnoreCase(currentStepName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return Rule supported class as required by WorldServer business rule framework
     */
    public Class getSupportedClass() {
        return Generic.class;
    }

    /**
     * auto-unclaim duration: Hours parameter
     */
    private WSClauseParameterDescriptor autoUnclaimDurationHoursParam =
            WSClauseParameterDescriptorFactory.createParameterDescriptor
                    (PARAM_CLAIMED_DURATION_HOURS,
                            "Auto-Unclaim Time Duration (hours)",
                            "Provide duration in hours before auto unclaim.",
                            WSClauseParameterDescriptorFactory.INTEGER
                    );

    /**
     * auto-unclaim duration: Minutes parameter
     */
    private WSClauseParameterDescriptor autoUnclaimDurationMinutesParam =
            WSClauseParameterDescriptorFactory.createParameterDescriptor
                    (PARAM_CLAIMED_DURATION_MINUTES,
                            "Auto-Unclaim Time Duration (minutes)",
                            "Provide duration in minutes before auto unclaim.",
                            WSClauseParameterDescriptorFactory.INTEGER
                    );

    /**
     * auto-unclaim step names, separated by comma (if more than one)
     */
    private WSClauseParameterDescriptor autoUnclaimStepsParam =
            WSClauseParameterDescriptorFactory.createParameterDescriptor
                    (PARAM_CLAIMED_STEPS,
                            "Auto-Unclaim Workflow Steps",
                            "Provide comma-separated list of Workflow Step names where tasks will be monitored for auto-unclaim.",
                            WSClauseParameterDescriptorFactory.STRING
                    );

    /**
     * reassign step names, separated by comma (if more than one)
     */
    private WSClauseParameterDescriptor autoReassignStepsParam =
            WSClauseParameterDescriptorFactory.createParameterDescriptor
                    (PARAM_REASSIGN_STEPS,
                            "Auto-Reassign Workflow Steps",
                            "Provide comma-separated list of Workflow Step names where tasks will be evaluated for auto-reassign.",
                            WSClauseParameterDescriptorFactory.STRING
                    );


    /**
     * @return Rule parameters
     */
    public WSClauseParameterDescriptor[] getParameters() {
        return new WSClauseParameterDescriptor[]{autoUnclaimDurationHoursParam,
                autoUnclaimDurationMinutesParam,
                autoUnclaimStepsParam,
                autoReassignStepsParam};
    }

    /**
     * @return Rule version
     */
    public String getVersion() {
        return "2.0";
    }

    /**
     * @return Rule name
     */
    public String getDisplayName(WSContext context) {
        return getName();
    }

    /**
     * @return Rule name
     */
    public String getName() {
        return "CI Business Rule";
    }

    /**
     * @return Rule description with parameter values
     */
    public String getDescription() {
        return "Automatically unclaim tasks at {2} (Translate and/or QC in CSV format) workflow steps " +
                "if claimed for longer than {0} hour(s) and {1} minutes. " +
                "Evaluate and reassign tasks at {3} (Translate and/or QC Queue in CSV format) workflow steps";

    }

    /**
     * @return Rule description
     */
    public String getDescription(WSContext context) {
        return getDescription();
    }


}

