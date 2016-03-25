package com.idiominc.ws.integration.compassion.utilities.reassignment;

//sdk
import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_OBJECT;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_TYPE;
import com.idiominc.ws.integration.compassion.utilities.metadata.AttributeValidator;
import com.idiominc.ws.integration.compassion.utilities.rating.*;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.attribute.WSAttributeValue;
import com.idiominc.wssdk.workflow.*;
import com.idiominc.wssdk.user.WSRole;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to check user availability and reassign steps
 *
 * [March, 2016] Enhancements added to support the dynamic reassignment where the rule will re-evaluate the step assignment.
 * Reassignment logic has been refactored and placed here to be shared with auto action and business rule.
 *
 * @author SDL Professional Services
 */
public class ReassignStep {

    //variables
    private final static String _ORIGINAL_LANGUAGE_ATTR = "OriginalLanguage";
    private static final String _TWOSTEP_PREFIX = "[Second Step Project] ";
    private static final String _SDLPROCESS_REQ = "SDLProcessRequired";
    private static final String _TWO_STEP_PROCESS = "TwoStepProcess";
    private static final String _TRANSLATION_PROCESS = "Translation";

    //regular expression to find user recorded in the attribute
    private static final String REGEXP = "\\[(.*?)\\]";

    //attribute that stores the most recent translator
    private static final String MOST_RECENT_TRANSLATOR_ATTR = "MostRecentTranslator";

    /**
     * Reassign the translation queue step
     * @param wsContext WorldServer Context
     * @param task task being reassigned
     * @param log WorldServer application log
     * @param returnMsg StringBuilder to pass output message to caller
     * @param hts human task step for reassignment
     * @param candidates preloaded candidate users for reassignment, if available; null if not preloaded
     * @param allUserRatingParsersTable preloaded table of all users' rating parsers; null if not preloaded
     * @param allLocaleUsers preloaded table of all users per locale; null if not preloaded
     * @param allRoleUsers preloaded table of all users per workflow role; null if not preloaded
     * @param allWorkgroupUsers preloaded table of all users per workgroup; null if not preloaded
     * @param allRuleIDTable preloaded table of all assignment rule per workgroup; null if not preloaded
     * @return true if reassign was successful; false otherwise
     */
    public static boolean reassignTranslationQueue(WSContext wsContext, WSAssetTask task, Logger log, StringBuilder returnMsg,
                                                   WSHumanTaskStep hts,
                                                   WSUser[] candidates,
                                                   Hashtable<Integer,UserRatingParser> allUserRatingParsersTable,
                                                   Hashtable<String,List<Integer>> allLocaleUsers,
                                                   Hashtable<String,List<Integer>> allRoleUsers,
                                                   Hashtable<String,List<Integer>> allWorkgroupUsers,
                                                   Hashtable<String, WBAssignmentRuleIdentifier> allRuleIDTable) {

        // performance capture
        long starttime = new Date().getTime();

        //data
        List<WSUser> resultingList = new ArrayList<>();
        StringBuilder assignedToSupervisors = new StringBuilder();

        if(null == hts) {
            log.error("Can not locate the very next human step to step " + task.getCurrentTaskStep().getWorkflowStep().getName());
            returnMsg.append("Can not locate the very next human step");
            return false;
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
            // If part of first-step process, then check the source locale from the payload for 'manual translation process'
            if(processRequired != null && processRequired.equals(_TRANSLATION_PROCESS) && !electronicContent) {
                sourceLocale = wsContext.getUserManager().getLocale(origSrcLocaleStr);
                if(sourceLocale == null) {
                    returnMsg.append("Expected source locale was not found: ").append(origSrcLocaleStr);
                    return false;
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
            returnMsg.append(e.getLocalizedMessage());
            return false;
        }

        WSRole translatorRole = wsContext.getUserManager().getRole(translatorRoleStr);
        WSRole supervisorRole = wsContext.getUserManager().getRole(supervisorRoleStr);

        if(null == translatorRole) {
            returnMsg.append("No workflow role ").append(translatorRoleStr).append(" has been found!");
            return false;
        }

        if(null == supervisorRole) {
            returnMsg.append("No workflow role ").append(supervisorRoleStr).append(" has been found!");
            return false;
        }

        WSAttributeValue complexityValue = project.getAttributeValue(Config._PROJECT_COMPLEXITY_ATTRIBUTE);
        if(null == complexityValue) {
            returnMsg.append("Complexity level is not stored under the project attribute "+Config._USER_RATING_ATTRIBUTE );
            return false;
        }

        String complexityStr = complexityValue.getAttributeValue();
        if(null == complexityStr) {
            returnMsg.append("Complexity level is null!");
            return false;
        }

        try {
            Integer.parseInt(complexityStr);
        } catch (NumberFormatException e) {
            log.error(e);
            returnMsg.append("Complexity level is not numeric ").append(complexityStr);
            return false;
        }

        try {
            WBAssignmentRuleIdentifier rid;
            if(allRuleIDTable != null) {
                rid = allRuleIDTable.get(workgroup.getName());
                if(rid == null) {
                    rid = new WBAssignmentRuleIdentifier(wsContext, workgroup);
                }
            } else {
                rid = new WBAssignmentRuleIdentifier(wsContext, workgroup);
            }
            List<RATING> acceptableRatings = rid.getListOfRatingsPerComplexity(RATING_APPLICABILITY.TRANSLATE,
                    complexityStr);

            if(candidates == null) {
                candidates = wsContext.getUserManager().getUsers();
            }

            for(WSUser candidate: candidates) {
                if(ReassignStep.belongs(candidate.getId(), targetLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), translatorRole, allRoleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), workgroup, allWorkgroupUsers)) {

                    try {
                        // expect RatingException, if user is not set with rating data; should not break the customization
                        log.info("Testing candidacy for " + candidate.getUserName() + " for " + sourceLocale.getName() + "-->" + targetLocale.getName());

                        // performance update: retrieved preloaded data if exists
                        UserRatingParser parser;
                        if(allUserRatingParsersTable != null) {
                            parser = allUserRatingParsersTable.get(candidate.getId());
                            if (parser == null) {
                                parser = new UserRatingParser(candidate, null);
                            }
                        } else {
                            parser = new UserRatingParser(candidate, null);
                        }

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
            returnMsg.append(e.getLocalizedMessage());
            return false;
        }

        if(0 == resultingList.size()) {
            log.info("Opting out to supervisor's role users");
            assignedToSupervisors.append("Assigned Translator Queue step to Supervisors - no qualified Translator found.");
            for(WSUser candidate: supervisorRole.getUsers()) {
                if(ReassignStep.belongs(candidate.getId(), targetLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), sourceLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), workgroup, allWorkgroupUsers))
                {
                    resultingList.add(candidate);
                }
            }
        }

        if(0 == resultingList.size()) {
            log.error("Not qualified users found - on both Translator and Supervisor roles!");
            returnMsg.append("Not qualified users found - in both Translator and Supervisor roles!");
            return false;
        }

        // performance capture
        long userEvaluationEndtime = new Date().getTime();
        long userEvaluationDuration = userEvaluationEndtime - starttime;

        if(!isUserAssigneeSame(hts, resultingList.toArray(new WSUser[resultingList.size()]))) {
            ReassignStep.reassign(hts, resultingList.toArray(new WSUser[resultingList.size()]));
            returnMsg.append("Reassigned step ").append(hts.getWorkflowStep().getName()).append(" to ").
                    append(resultingList.size()).append(" qualified users. ").
                    append(assignedToSupervisors.toString());
        }

        // performance capture
        long endtime = new Date().getTime();
        long assignmentDuration = endtime - userEvaluationEndtime;
        long totalDuration = endtime - starttime;
        log.debug(returnMsg.toString() + "Total:" + totalDuration +
        " User Evaluation:" + userEvaluationDuration +
        " Reassignment:" + assignmentDuration);

        // otherwise user assignees are same; no assignment is needed
        // do nothing; only output message when there is actual reassignment

        return true;
    }

    /**
     * Compare the current step user assignees against new users list and return true if the assignees list is same; false otherwise
     * @param hts Human task step
     * @param newUsers New user assignees
     * @return true if current user assignee list is same; false otherwise
     */
    private static boolean isUserAssigneeSame(WSHumanTaskStep hts, WSUser[] newUsers) {
        WSUser[] currentAssignees = hts.getUserAssignees();
        if(currentAssignees == null || currentAssignees.length == 0) {
            return false;
        }
        if(currentAssignees.length != newUsers.length) {
            return false;
        }
        Hashtable<Integer,WSUser> table = new Hashtable<>();
        for (WSUser currentAssignee : currentAssignees) {
            table.put(currentAssignee.getId(), currentAssignee);
        }
        for (WSUser newUser : newUsers) {
            if (table.get(newUser.getId()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reassign the QC Queue step
     * @param wsContext WorldServer Context
     * @param task task being reassigned
     * @param log WorldServer application log
     * @param returnMsg StringBuilder to pass output message to caller
     * @param parent parent object for use with reassignment
     * @param hts human task step for reassignment
     * @param candidates preloaded candidate users for reassignment, if available; null if not preloaded
     * @param allUserRatingParsersTable preloaded table of all users' rating parsers; null if not preloaded
     * @param allLocaleUsers preloaded table of all users per locale; null if not preloaded
     * @param allRoleUsers preloaded table of all users per workflow role; null if not preloaded
     * @param allWorkgroupUsers preloaded table of all users per workgroup; null if not preloaded
     * @param allRuleIDTable preloaded table of all assignment rule per workgroup; null if not preloaded
     * @return true if reassign was successful; false otherwise
     */
    public static boolean reassignQCQueue(WSContext wsContext, WSAssetTask task, Logger log, StringBuilder returnMsg, Object parent,
                                          WSHumanTaskStep hts,
                                          WSUser[] candidates,
                                          Hashtable<Integer,UserRatingParser> allUserRatingParsersTable,
                                          Hashtable<String,List<Integer>> allLocaleUsers,
                                          Hashtable<String,List<Integer>> allRoleUsers,
                                          Hashtable<String,List<Integer>> allWorkgroupUsers,
                                          Hashtable<String, WBAssignmentRuleIdentifier> allRuleIDTable) {

        // performance capture
        long starttime = new Date().getTime();

        //data
        List<WSUser> resultingList = new ArrayList<>();
        StringBuilder assignedToSupervisors = new StringBuilder();

        if(null == hts) {
            log.error("Can not locate the very next human step to step " + task.getCurrentTaskStep().getWorkflowStep().getName());
            returnMsg.append("Can not locate the very next human step");
            return false;
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
            // If part of first-step process, then check the source locale from the payload for 'manual translation process'
            if(processRequired != null && processRequired.equals(_TRANSLATION_PROCESS) && !electronicContent) {
                sourceLocale = wsContext.getUserManager().getLocale(origSrcLocaleStr);
                if(sourceLocale == null) {
                    returnMsg.append("Expected source locale was not found: ").append(origSrcLocaleStr);
                    return false;
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
            returnMsg.append(e.getLocalizedMessage());
            return false;
        }

        WSRole translatorRole = wsContext.getUserManager().getRole(translatorRoleStr);
        WSRole supervisorRole = wsContext.getUserManager().getRole(supervisorRoleStr);

        if(null == translatorRole) {
            returnMsg.append("No workflow role ").append(translatorRoleStr).append(" has been found!");
            return false;
        }

        if(null == supervisorRole) {
            returnMsg.append("No workflow role ").append(supervisorRoleStr).append(" has been found!");
            return false;
        }

        WSAttributeValue complexityValue = project.getAttributeValue(Config._PROJECT_COMPLEXITY_ATTRIBUTE);
        if(null == complexityValue) {
            returnMsg.append("Complexity level is not stored under the project attribute "+ Config._USER_RATING_ATTRIBUTE );
            return false;
        }

        String complexityStr = complexityValue.getAttributeValue();
        if(null == complexityStr) {
            returnMsg.append("Complexity level is null!");
            return false;
        }

        try {
            Integer.parseInt(complexityStr);
        } catch (NumberFormatException e) {
            log.error(e);
            returnMsg.append("Complexity level is not numeric ").append(complexityStr);
            return false;
        }

        try {
            String qcQualifiedAttributeName;
            WSUser realTranslator = findTranslator(wsContext, task.getProject(), log);
            WBAssignmentRuleIdentifier rid;
            if(allRuleIDTable != null) {
                rid = allRuleIDTable.get(workgroup.getName());
                if(rid == null) {
                    rid = new WBAssignmentRuleIdentifier(wsContext, workgroup);
                }
            } else {
                rid = new WBAssignmentRuleIdentifier(wsContext, workgroup);
            }
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
                returnMsg.append("Attributes Misconfiguration. ").append(e.getLocalizedMessage());
                return false;
            }

            if(candidates == null) {
                candidates = wsContext.getUserManager().getUsers();
            }

            for(WSUser candidate: candidates) {
                boolean isUserQualified = WSAttributeUtils.getBooleanAttribute(parent, candidate, qcQualifiedAttributeName);
                if(!isUserQualified) {
                    log.info("User " + candidate.getUserName() + " is disqualified for QC as defined in User setting");
                    continue;
                }
                if(realTranslator != null
                        &&
                        realTranslator.getId() == candidate.getId()) {
                    //do not assign QC step to a translator who did original translation
                    log.info("User " + candidate.getUserName() + " is disqualified for QC. Did translation");
                    continue;
                }
                if(ReassignStep.belongs(candidate.getId(), targetLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), sourceLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), translatorRole, allRoleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), workgroup, allWorkgroupUsers)) {

                    try {
                        // performance update: retrieved preloaded data if exists
                        UserRatingParser parser;
                        if(allUserRatingParsersTable != null) {
                            parser = allUserRatingParsersTable.get(candidate.getId());
                            if (parser == null) {
                                parser = new UserRatingParser(candidate, null);
                            }
                        } else {
                            parser = new UserRatingParser(candidate, null);
                        }
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
            returnMsg.append(e.getLocalizedMessage());
            return false;
        }

        if(0 == resultingList.size()) {
            log.info("Opting out to supervisor's role users");
            assignedToSupervisors.append("Assigned QC Queue step to Supervisors - no qualified Translator has been found.");
            WSUser realTranslator = findTranslator(wsContext, task.getProject(), log);
            for(WSUser candidate: supervisorRole.getUsers()) {
                if(realTranslator != null
                        &&
                        realTranslator.getId() == candidate.getId()) {
                    //do not assign the step to supervisor if one did the translation
                    log.info("User " + candidate.getUserName() + " is disqualified for QC. Did translation");
                    continue;
                }
                if(ReassignStep.belongs(candidate.getId(), targetLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), sourceLocale, allLocaleUsers)
                        &&
                        ReassignStep.belongs(candidate.getId(), workgroup, allWorkgroupUsers)) {
                    resultingList.add(candidate);
                }
            }
        }

        if(0 == resultingList.size()) {
            log.error("Not qualified users found - on both Translator and Supervisor roles!");
            returnMsg.append("Not qualified users found - in both Translator and Supervisor roles!");
            return false;
        }

        // performance capture
        long userEvaluationEndtime = new Date().getTime();
        long userEvaluationDuration = userEvaluationEndtime - starttime;

        if(!isUserAssigneeSame(hts, resultingList.toArray(new WSUser[resultingList.size()]))) {
            ReassignStep.reassign(hts, resultingList.toArray(new WSUser[resultingList.size()]));
            returnMsg.append("Reassigned step ").append(hts.getWorkflowStep().getName()).append(" to ").
                    append(resultingList.size()).append(" qualified users. ").
                    append(assignedToSupervisors.toString());
        }

        // performance capture
        long endtime = new Date().getTime();
        long assignmentDuration = endtime - userEvaluationEndtime;
        long totalDuration = endtime - starttime;
        log.debug(returnMsg.toString() + "Total:" + totalDuration +
                " User Evaluation:" + userEvaluationDuration +
                " Reassignment:" + assignmentDuration);

        // otherwise users are same; no assignment is needed
        // do nothing; only output message for when there is actual reassignment

        return true;
    }

    /**
     * Locate the most recent translator on the project
     * @param context - WS Context
     * @param project - current project
     * @return  the most recent translator or NULL if not found
     */
    public static WSUser findTranslator(WSContext context, WSProject project, Logger log)  {
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
     * Check if user belongs to the locale's user
     * @param userID - user ID
     * @param locale - WorldServer locale
     * @return true if user belongs to the locale's users
     */
    public static boolean belongs(int userID, WSLocale locale, Hashtable<String,List<Integer>> allUsers) {

        if(allUsers != null) {
            List<Integer> userIds = allUsers.get(locale.getName());
            return find(userID, userIds);
        } else {
            WSUser[] lcUsers = locale.getUsers();
            if(null == lcUsers || lcUsers.length == 0) {
                return false;
            }
            for(WSUser u: lcUsers) {
                if(u.getId() == userID) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Check if user belongs to the role's user
     * @param userID - user ID
     * @param role - WorldServer role
     * @return true if user belongs to the role's users
     */
    public static boolean belongs(int userID, WSRole role, Hashtable<String,List<Integer>> allUsers) {

        if(allUsers != null) {
            List<Integer> userIds = allUsers.get(role.getName());
            return find(userID, userIds);
        } else {
            WSUser[] rlUsers = role.getUsers();
            if(null == rlUsers || rlUsers.length == 0) {
                return false;
            }
            for(WSUser u: rlUsers) {
                if(u.getId() == userID) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Check if user belongs to the workgroup's user
     * @param userID - user ID
     * @param workgroup - WorldServer workgroup
     * @return true if user belongs to the workgroup's users
     */
    public static boolean belongs(int userID, WSWorkgroup workgroup, Hashtable<String,List<Integer>> allUsers) {

        if(allUsers != null) {
            List<Integer> userIds = allUsers.get(workgroup.getName());
            return find(userID, userIds);
        } else {
            WSUser[] wgUsers = workgroup.getUsers();
            if(null == wgUsers || wgUsers.length == 0) {
                return false;
            }
            for(WSUser u: wgUsers) {
                if(u.getId() == userID) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Search and return true if given userId is found in the list of IDs
     * @param userId target user ID
     * @param userIds list of user IDs to search from
     * @return true if userId is found in the given list
     */
    private static boolean find(int userId, List<Integer> userIds) {
        for (Integer id : userIds) {
            if (userId == id)
                return true;
        }
        return false;
    }

    /**
     * Reassign step to the list of users
     * @param step - step to reassign
     * @param users - users to whom step should be assigned to
     */
    public static boolean reassign(WSHumanTaskStep step, WSUser[] users) {

        final List<WSUser> usersList = new ArrayList<>();
        Collections.addAll(usersList, users);
        WSRole[] currentRoleAssignees = step.getRoleAssignees();
        step.setUserAssignees(usersList.toArray(new WSUser[usersList.size()]));
        if(currentRoleAssignees != null && currentRoleAssignees.length > 0)
            step.setRoleAssignees(new WSRole[0]);

        return true;
    }

    /**
     * Obtain user who has completed last human task step
     * @param task - worldserver task
     * @return User, or nulkl if not found
     */
    public static WSUser getPrevousTaskStepExecutor(WSTask task) {
      WSTaskHistory history = task.getTaskHistory();
      if(null == history) {
          return null;
      }
      return history.getLastHumanStepUser();
    }

    /**
     * Obtain the very next human step in the workflow
      * @param task - WorldServer's task object
     *  @return Next human task step, or null if not found
     */
    public static WSHumanTaskStep getNextHumanTaskStep(WSTask task) {
        WSTaskStep current = task.getCurrentTaskStep();
        WSTaskStep next = current.getNextTaskStep(current.getWorkflowStep().getTransitions()[0]);
        while(next != null && !(next instanceof WSHumanTaskStep)) {
            next = next.getNextTaskStep(next.getWorkflowStep().getTransitions()[0]);
        }
        if(null == next) {
            return null;
        }
        return (WSHumanTaskStep) next;

    }

}
