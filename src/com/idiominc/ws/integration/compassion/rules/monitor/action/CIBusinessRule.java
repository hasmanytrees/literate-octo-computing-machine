package com.idiominc.ws.integration.compassion.rules.monitor.action;

import com.idiominc.ws.integration.compassion.rules.nocondition.Generic;
import com.idiominc.ws.integration.compassion.utilities.rating.RatingException;
import com.idiominc.ws.integration.compassion.utilities.rating.UserRatingParser;
import com.idiominc.ws.integration.compassion.utilities.rating.WBAssignmentRuleIdentifier;
import com.idiominc.ws.integration.compassion.utilities.reassignment.ReassignStep;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.rule.WSActionClauseComponent;
import com.idiominc.wssdk.component.rule.WSActionClauseResults;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptor;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptorFactory;
import com.idiominc.wssdk.component.rule.parameter.WSIntegerClauseParameterValue;
import com.idiominc.wssdk.component.rule.parameter.WSStringClauseParameterValue;
import com.idiominc.wssdk.rule.WSRule;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSRole;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.*;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.util.*;

/**
 * [March, 2016] Enhancements added to support the dynamic reassignment where the rule will re-evaluate the step assignment.
 * This change will support the scenario where projects are already created in WorldServer, and then new users are added to the system.
 * Traditionally these users are not assigned to previously allocated step.
 * This rule will compare the current list of assignees and update it if new assignees are found.
 *
 * @author SDL Professional Services
 */
public class CIBusinessRule extends WSActionClauseComponent {

    //log
    private Logger log = Logger.getLogger(CIBusinessRule.class);

    // rule parameters
    public static String PARAM_REASSIGN_STEPS = "reassignSteps";
    public static String PARAM_REASSIGN_BATCH = "reassignBatch";

    // Rule default step names
    public static final String STEP_TRANSLATION_QUEUE = "Translation Queue";
    public static final String STEP_QC_QUEUE = "QC Queue";

    /*
     * Identify a window of project IDs which will be processed by inner transaction to process for reassignment
     *
     * @param context - WorldServer Context
     * @param rule - WorldServer Rule for this given rule action clause
     * @param objects - WorldServer objects against which the rule is being executed
     * @param parameters - List of parameters included with the rule definition
     */
    public WSActionClauseResults execute(final WSContext context, final WSRule rule, final Set objects, final Map parameters) {

        // retrieve batch input parameter
        Integer reassignBatchParamValue = ((WSIntegerClauseParameterValue) parameters.get(reassignBatchParam.getName())).getValue();
        int maxWindow = (reassignBatchParamValue > 1000) ? 1000 : reassignBatchParamValue;

        // setup project IDs for reassignment check in batch size
        final WSUser[] updatedUsers = retrieveUpdatedUsers(context);
        if(updatedUsers.length == 0) {
            // no affected users; no need to run the rule!
            return new WSActionClauseResults("Done. No user has been updated for reassignment.");
    }
        WSProject[] affectedProjects = retrieveAffectedProjects(context, updatedUsers);

        List<List<Integer>> aggregateWindowedProjectIds = new ArrayList<>();
        for (WSProject project : affectedProjects) {
            if (aggregateWindowedProjectIds.size() == 0 || aggregateWindowedProjectIds.get(aggregateWindowedProjectIds.size() - 1).size() >= maxWindow) {
                aggregateWindowedProjectIds.add(new ArrayList<Integer>());
            }
            aggregateWindowedProjectIds.get(aggregateWindowedProjectIds.size() - 1).add(project.getId());
        }

        // perform the business rule for each batch at a time
        final Integer[] resultsCounter = new Integer[2];
        int reassignCounter = 0;
        for (final List<Integer> windowedProjectIdTx : aggregateWindowedProjectIds) {
            WSContextManager.run(context, new WSRunnable() {
                @Override
                public boolean run(WSContext context) {
                    _internalTx_executeWithProjects(context, parameters, windowedProjectIdTx, resultsCounter);
                    return true;
                }
            });
            reassignCounter = reassignCounter + resultsCounter[0];
        }

        resetUserChangedAttribute(updatedUsers);

        return new WSActionClauseResults("Done:" + "Reassigned:[" + reassignCounter + "].");
    }

    /**
     * Retrieve list of users that have been changed via SAML and/or user rating UI
     * @param context WorldServer context
     * @return array of users
     */
    private WSUser[] retrieveUpdatedUsers(WSContext context) {
        WSUser[] users = context.getUserManager().getUsers();
        List<WSUser> usersList = new ArrayList<>();
        for(WSUser user : users) {
            String userChangedStr = user.getAttribute("UserChanged");
            if(userChangedStr != null && userChangedStr.equals("true")) {
                usersList.add(user);
            }
        }
        return usersList.toArray(new WSUser[usersList.size()]);
    }

    private void resetUserChangedAttribute(WSUser[] updatedUsers) {
        // reset users if all reassignment has been completed: Currently under investigation for future enhancements
        for(WSUser user : updatedUsers) {
            user.setAttribute("UserChanged", "false");
        }
   }

    private WSProject[] retrieveAffectedProjects(WSContext context, final WSUser[] updatedUsers) {
        Hashtable<Integer, WSProject> projectIdTable = new Hashtable<>();
        Hashtable<String,WSLocale> localeTable = new Hashtable<>();

        for(WSUser user : updatedUsers) {
            WSProject[] assignedProjects = context.getWorkflowManager().getAssignedProjectsForUser(user);
            addProjectListToTable(assignedProjects, projectIdTable);
            WSLocale[] userLocales = user.getLocales();
            addLocalesToTable(userLocales, localeTable);
        }

        for (WSLocale locale : localeTable.values()) {
            WSProject[] localeProjects = context.getWorkflowManager().getProjectsForLocale(locale, (new WSProjectStatus[]{WSProjectStatus.ACTIVE}));
            addProjectListToTable(localeProjects, projectIdTable);
        }

        return projectIdTable.values().toArray(new WSProject[projectIdTable.size()]);
    }

    private void addLocalesToTable(WSLocale[] userLocales, Hashtable<String, WSLocale> localeTable) {
        for(WSLocale locale : userLocales) {
            localeTable.put(locale.getName(), locale);
        }
    }

    private void addProjectListToTable(WSProject[] assignedProjects, Hashtable<Integer, WSProject> projectIds) {
        for(WSProject project : assignedProjects) {
            projectIds.put(project.getId(), project);
        }
    }

    /**
     * Retrieve projects by the ID
     * @param context WorldServer context
     * @param projectIds to-be retrieved project IDs
     * @return array of WSProject objects from given project IDs
     */
    private WSProject[] getProjectsById(WSContext context, List<Integer> projectIds) {
        List<WSProject> ret = new ArrayList<>();
        for (int pid : projectIds) {
            ret.add(context.getWorkflowManager().getProject(pid));
        }

        return ret.toArray(new WSProject[ret.size()]);
    }

    /**
     * Execute the rule; identify the given parameters, and parse through the list of active tasks in the system,
     * and identify tasks that have been in "Accepted/Claimed" state for longer than expected, and put them
     * back into appropriate Queue steps.
     *
     * @param context WorldServer context
     * @param parameters input parameters
     * @param projectIds project IDs to be processed for given batch
     * @param resultsCounter return value for processed reassignment
     * @return true if execution is successful
     */
    public boolean _internalTx_executeWithProjects(final WSContext context, final Map parameters, List<Integer> projectIds, final Integer[] resultsCounter) {

        // retrieve the partial list of projects from given project IDs
        WSProject[] partialProjects = getProjectsById(context, projectIds);

        // performance capture
        long starttime = new Date().getTime();

        // ********************************* Prepare Parameters

        // retrieve CSV-formatted reassign step names parameter
        String reassignStepNamesParamStr = ((WSStringClauseParameterValue) parameters.get(
                autoReassignStepsParam.getName())).getValue();
        String[] allReassignStepNamesParam;
        if (reassignStepNamesParamStr.contains(",")) {
            allReassignStepNamesParam = reassignStepNamesParamStr.split(",");
        } else {
            allReassignStepNamesParam = new String[]{reassignStepNamesParamStr};
        }

        // ********************************* Performance Preload data
        // performance: preload all users and user rating info
        XMLReader xmlReader;
        try {
            xmlReader = XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            log.error(e.getMessage());
            return false;
        }

        // Retrieving a subset of users list is currently being investigated for future enhancements
        WSUser[] allUsers = context.getUserManager().getUsers();
        Hashtable<Integer, UserRatingParser> allUserRatingParsersTable = new Hashtable<>();
        for (WSUser user : allUsers) {
            try {
                UserRatingParser parser = new UserRatingParser(user, xmlReader);
                allUserRatingParsersTable.put(user.getId(), parser);
            } catch (RatingException e) {
                // Some users may not be setup with rating; that's fine we'll skip such users
            }
        }

        // performance: preload all required user information
        WSLocale[] allLocales = context.getUserManager().getLocales();
        Hashtable<String, List<Integer>> allLocaleUsers = new Hashtable<>();
        for (WSLocale aLocale : allLocales) {

            WSUser[] localeUsers = aLocale.getUsers();
            List<Integer> userIds = new ArrayList<>();
            for (WSUser user : localeUsers) {
                userIds.add(user.getId());
            }
            allLocaleUsers.put(aLocale.getName(), userIds);
        }

        WSRole[] allRoles = context.getUserManager().getRoles();
        Hashtable<String, List<Integer>> allRoleUsers = new Hashtable<>();
        for (WSRole aRole : allRoles) {
            WSUser[] users = aRole.getUsers();
            List<Integer> userIds = new ArrayList<>();
            for (WSUser user : users) {
                userIds.add(user.getId());
            }
            allRoleUsers.put(aRole.getName(), userIds);
        }

        WSWorkgroup[] allWorkgroups = context.getUserManager().getWorkgroups();
        Hashtable<String, List<Integer>> allWorkgroupUsers = new Hashtable<>();
        Hashtable<String, WBAssignmentRuleIdentifier> allRuleIDTable = new Hashtable<>();
        for (WSWorkgroup aWorkgroup : allWorkgroups) {
            WSUser[] users = aWorkgroup.getUsers();
            List<Integer> userIds = new ArrayList<>();
            for (WSUser user : users) {
                userIds.add(user.getId());
            }
            allWorkgroupUsers.put(aWorkgroup.getName(), userIds);
            try {
                WBAssignmentRuleIdentifier rid = new WBAssignmentRuleIdentifier(context, aWorkgroup);
                allRuleIDTable.put(aWorkgroup.getName(), rid);
            } catch (RatingException e) {
                log.error(e.getMessage());
                //continue with next workgroup
            }
        }

        // Performance capture
        long reassignTotalTime = 0;
        int reassignCount = 0;

        try {
            // Iterate through all active projects in the system in given batch, and identify tasks that need to be pushed back to Queue/reassigned
            for (WSProject project : partialProjects) {
                // Iterate through all active tasks in the system, for each active projects
                for (WSTask task : project.getActiveTasks()) {
                    WSAssetTask assetTask = (WSAssetTask) task;
                    String currentStepName = assetTask.getCurrentTaskStep().getWorkflowStep().getName();

                    // Perform the reassign check only for given specified workflow step
                    // [Enhancement March, 2016] This portion handles dynamic reassignment with shared reassignment logic
                    if (currentStepName != null && stepFound(currentStepName, allReassignStepNamesParam)) {

                        // performance capture
                        long reassignStartTime = new Date().getTime();

                        StringBuilder returnMsg = new StringBuilder();
                        boolean success = false;
                        WSHumanTaskStep currentStep = (WSHumanTaskStep) assetTask.getCurrentTaskStep();
                        if (currentStepName.equals(STEP_TRANSLATION_QUEUE)) {
                            success = ReassignStep.reassignTranslationQueue(context, assetTask, log, returnMsg, currentStep, allUsers, allUserRatingParsersTable,
                                    allLocaleUsers, allRoleUsers, allWorkgroupUsers, allRuleIDTable);
                        } else if (currentStepName.equals(STEP_QC_QUEUE)) {
                            success = ReassignStep.reassignQCQueue(context, assetTask, log, returnMsg, this, currentStep, allUsers, allUserRatingParsersTable,
                                    allLocaleUsers, allRoleUsers, allWorkgroupUsers, allRuleIDTable);
                        }

                        // Increment the reassignment counter if the reassignment was a success
                        if (success) {
                            if (!returnMsg.toString().isEmpty()) {
                                reassignCount++;
                            }
                            // otherwise nothing is needed; only output message for actual reassignment
                        }
                        else {
                            log.info("Project[" + project.getId() + "] was not reassigned:" + returnMsg.toString() + "<br><br>");
                        }

                        // performance capture
                        long reassignEndTime = new Date().getTime();
                        reassignTotalTime = reassignTotalTime + (reassignEndTime - reassignStartTime);
                    }
                    // otherwise the current step does not match the target step to check for claimant or reassign; continue with rest of tasks
                }
            }
            if (reassignCount > 0) {
                log.info(" Reassigned " + reassignCount + " projects successfully.<br>\n");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        long endtime = new Date().getTime();
        log.info("[" + (endtime - starttime) / 1000.00 + "s]" +
                "[Reassign:" + reassignTotalTime / 1000.00 + "s].");

        // set return values
        resultsCounter[0] = reassignCount;

        return true;
    }

    /**
     * Identify the step to check for the rule from the step names parameter input
     *
     * @param currentStepName current workflow step name
     * @param allStepNamesStr all workflow step names to check for the rule from parameter
     * @return true if current step needs to be checked; false otherwise.
     */
    private boolean stepFound(String currentStepName, String[] allStepNamesStr) {

        for (String stepName : allStepNamesStr) {
            if (stepName.equalsIgnoreCase(currentStepName)) {
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
     * reassign batch count: input parameter
     */
    private WSClauseParameterDescriptor reassignBatchParam =
            WSClauseParameterDescriptorFactory.createParameterDescriptor
                    (PARAM_REASSIGN_BATCH,
                            "Reassign batch count (projects)",
                            "Provide batch count for project reassignment.",
                            WSClauseParameterDescriptorFactory.INTEGER
                    );


    /**
     * @return Rule parameters
     */
    public WSClauseParameterDescriptor[] getParameters() {
        return new WSClauseParameterDescriptor[]{
                autoReassignStepsParam,
                reassignBatchParam};
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
        return "Evaluate and reassign tasks at {0} (Translate and/or QC Queue in CSV format) workflow steps " +
                "in batch of {1} projects";

    }

    /**
     * @return Rule description
     */
    public String getDescription(WSContext context) {
        return getDescription();
    }


}

