package com.idiominc.ws.integration.compassion.authenticator.saml;

import com.idiominc.ws.integration.compassion.utilities.WSUtil;
import com.idiominc.ws.integration.profserv.commons.sso.SSOUser;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.linguistic.WSLanguage;
import com.idiominc.wssdk.user.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Compassion SSO user object utilized by the supporting SAML authentication to authenticate and update
 *
 * @author SDL Professional Services
 */
public class CISSOUser extends SSOUser {

    private static Logger log = Logger.getLogger(CISSOUser.class);

    private String[] workGroups;
    private String[] locales;
    private String[] workFlowRoles;
    private String UIDisplayLanguage;
    private String regionalSetting;
    private String[] clients;

    public CISSOUser(String id,
                     String userName,
                     String email,
                     String firstName,
                     String lastName,
                     String userType,
                     String workGroupsCSV,
                     String localesCSV,
                     String workFlowRolesCSV,
                     String UIDisplayLanguage,
                     String regionalSetting,
                     String clientsCSV) {

        super(id, userName, email, firstName, lastName, userType);

        this.workGroups = workGroupsCSV.split(",");
        this.locales = localesCSV.split(",");
        this.workFlowRoles = workFlowRolesCSV.split(",");
        this.UIDisplayLanguage = UIDisplayLanguage;
        this.regionalSetting = regionalSetting;
        this.clients = clientsCSV.split(",");
    }

    /**
     * Method called when SSO User logs in
     *
     * @param context - WorldServer context
     * @param ssoUser - The user attempting to login
     * @return - Always returns true
     */
    public boolean update(WSContext context, WSUser ssoUser) {

        /** Call the SSOUser super update method */
        super.update(context, ssoUser);

        // If the user doesn't have a valid "UserChanged" attribute value then it should be true
        if(ssoUser.getAttribute("UserChanged") == null) {
            ssoUser.setAttribute("UserChanged", "true");
        }

        // Get the default value from the attribute
        String defaultRule = context.getAttributeManager().getAttributeDescriptor( WSUser.class,
                "UserChanged").getDefaultValue();

        // Check to see if it exists
        if (defaultRule == null) {

            // if it doesn't then set the default value
            context.getAttributeManager().getAttributeDescriptor( WSUser.class,
                    "UserChanged").setDefaultValue("true");
        }

        // todo: Should this be in here or in base SSO class?
        if (!getUserType().equals(ssoUser.getUserType().getName())) {
            WSUserType uType = context.getUserManager().getUserType(getUserType());
            ssoUser.setUserType(uType);
        }

        updateUserGroup(context, ssoUser, WSWorkgroup.class, this.workGroups);
        updateUserGroup(context, ssoUser, WSLocale.class, this.locales);
        updateUserGroup(context, ssoUser, WSRole.class, this.workFlowRoles);
        updateUserGroup(context, ssoUser, WSClient.class, this.clients);

        /*List<String> newWorkGroups = getGroupDifference(ssoUser.getWorkgroups(), this.workGroups);

        if (newWorkGroups != null) {
            for (String workGroupName : getGroupDifference(ssoUser.getWorkgroups(), this.workGroups)) {

                WSWorkgroup workgroup = context.getUserManager().getWorkgroup(workGroupName);

                if (workgroup != null) {
                    ssoUser.addToGroup(workgroup);
                } else {
                    log.warn("Workgroup " + workGroupName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        List<String> newLocales = getGroupDifference(ssoUser.getLocales(), this.locales);

        if (newLocales != null) {

            for (String localeName : getGroupDifference(ssoUser.getLocales(), this.locales)) {

                WSLocale locale = context.getUserManager().getLocale(localeName);

                if (locale != null) {
                    ssoUser.addToGroup(locale);
                } else {
                    log.warn("Locale " + localeName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        List<String> newWorkFlowRoles = getGroupDifference(ssoUser.getRoles(), this.workFlowRoles);

        if (newWorkFlowRoles != null) {
            for (String workFlowRoleName : newWorkFlowRoles) {

                WSRole workFlowRole = context.getUserManager().getRole(workFlowRoleName);

                if (workFlowRole != null) {
                    ssoUser.addToGroup(workFlowRole);
                } else {
                    log.warn("WorkFlowRole " + workFlowRoleName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        List<String> newClients = getGroupDifference(ssoUser.getClients(), this.clients);

        if (newClients != null) {
            for (String clientName : getGroupDifference(ssoUser.getClients(), this.clients)) {

                WSClient client = context.getUserManager().getClient(clientName);

                if (client != null) {
                    ssoUser.addToGroup(client);
                } else {
                    log.warn("Client " + clientName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }
        */

        /** Loop through regional languages and see if any match requested */
        WSLanguage regionalSettingLanguage = null;

        for (WSLanguage language : context.getLinguisticManager().getLanguages()) {
            if (language.getName().equals(this.regionalSetting) &&
                    ssoUser.getRegionalSettingsLanguage().getName() != this.regionalSetting) {
                regionalSettingLanguage = language;
            }
        }

        /** Loop through display languages and see if any match requested */
        WSLanguage UILanguage = null;
        for (WSLanguage language : context.getLinguisticManager().getAvailableDisplayLanguages()) {
            if (language.getName().equals(this.UIDisplayLanguage) &&
                    ssoUser.getDisplayLanguage().getName() != this.regionalSetting) {
                UILanguage = language;
            }
        }

        /** Set the regional language */
        if (regionalSettingLanguage != null) {
            ssoUser.setRegionalSettingsLanguage(regionalSettingLanguage);
        } else {
            log.warn("Regional Language " + regionalSettingLanguage + " does not exist for user " + ssoUser.getUserName() + ".");
        }

        /** Set the display language */
        if (UILanguage != null) {
            ssoUser.setDisplayLanguage(UILanguage);
        } else {
            log.warn("UI Language " + UILanguage + " does not exist for user " + ssoUser.getUserName() + ".");
        }

        return true;
    }

    /**
     * Get a list of WSGroups that differ from a list of string names corresponding to WS group names
     *
     * @param groups     - An array of WorldServer groups
     * @param userGroups - An array of group names
     * @return - A list of strings containing any groups names not in the exsting groups
     */
    /*
    private List<String> getGroupDifference(WSGroup[] groups, String[] userGroups) {

        List<String> ret = new ArrayList<String>();
        int groupExists = 0;

        for (String userGroup : userGroups) {
            for (WSGroup group : groups) {
                if (userGroup.equals(group.getName())) {
                    groupExists++;
                }
            }

            if (groupExists == 0) {
                ret.add(userGroup);
            }
        }

        return ret;
    }
    */

    /**
     *
     * Updates the WorldServer user groups (role,locale, work-group and client) to the new values based by name. Only
     * modified the group assignment if the value changes -- we must avoid changing the assignment if it is not necessary
     * as it can be a time consuming operation. For large users, we may need to ensure this is a background process
     *
     * @param context                  WorldServer context with all services of WS
     * @param user                     The user to change assignments for
     * @param type                     The class type of the user group assignments
     * @param updatedAssignmentsByName The group assignments -- by name -- to ensure are assigned to the user after login
     * @return True if the user was updated; otherwise false
     */
    private boolean updateUserGroup(WSContext context, WSUser user, Class type, String[] updatedAssignmentsByName) {

        List<WSGroup> toAdd = new ArrayList<WSGroup>();
        List<WSGroup> toRemove = new ArrayList<WSGroup>();

        WSGroup[] currentAssignments = getGroupByUser(user, type);
        WSGroup[] updatedAssignments = getGroupByName(context, updatedAssignmentsByName, type);

        // we use 'hasValue' due to a bug in the hashcode of some objects
        for (WSGroup updatedAssignment : updatedAssignments) {
            if (!WSUtil.hasValue(updatedAssignment, currentAssignments)) {
                toAdd.add(updatedAssignment);
            }
        }

        for (WSGroup currentAssignment : currentAssignments) {
            if (!WSUtil.hasValue(currentAssignment, updatedAssignments)) {
                toRemove.add(currentAssignment);
            }
        }

        if (toAdd.size() == 0 && toRemove.size() == 0) {
            return false;
        }

        for (WSGroup g : toAdd) {
            user.addToGroup(g);
        }

        for (WSGroup g : toRemove) {
            user.removeFromGroup(g);
        }

        // Indicate that the user has changed for the dynamic assignment rule
        user.setAttribute("UserChanged", "true");

        return true;
    }

    public WSGroup[] getGroupByName(WSContext context, String[] byNames, Class type) {
        List<WSGroup> ret = new ArrayList<WSGroup>();
        for (String byName : byNames) {

            WSGroup toAdd = null;

            if (WSLocale.class.isAssignableFrom(type)) {
                toAdd = context.getUserManager().getLocale(byName);
            } else if (WSRole.class.isAssignableFrom(type)) {
                toAdd = context.getUserManager().getRole(byName);
            } else if (WSWorkgroup.class.isAssignableFrom(type)) {
                toAdd = context.getUserManager().getWorkgroup(byName);
            } else if (WSClient.class.isAssignableFrom(type)) {
                toAdd = context.getUserManager().getClient(byName);
            } else {
                throw new IllegalArgumentException("Unknown group type: " + type.getClass());
            }

            if (toAdd == null) {
                log.warn("Group type " + type.getClass() + byName + " does not exist.");
            } else {
                ret.add(toAdd);
            }

        }

        return ret.toArray(new WSGroup[ret.size()]);
    }

    public WSGroup[] getGroupByUser(WSUser user, Class type) {
        if (WSLocale.class.isAssignableFrom(type)) {
            return user.getLocales();
        } else if (WSRole.class.isAssignableFrom(type)) {
            return user.getRoles();
        } else if (WSWorkgroup.class.isAssignableFrom(type)) {
            return user.getWorkgroups();
        } else if (WSClient.class.isAssignableFrom(type)) {
            return user.getClients();
        } else {
            throw new IllegalArgumentException("Unknown group type: " + type.getClass());
        }
    }
}
