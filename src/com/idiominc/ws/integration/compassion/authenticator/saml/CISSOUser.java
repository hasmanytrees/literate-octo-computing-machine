package com.idiominc.ws.integration.compassion.authenticator.saml;

import com.idiominc.ws.integration.profserv.commons.sso.SSOUser;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.linguistic.WSLanguage;
import com.idiominc.wssdk.user.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cslack on 9/16/2015.
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

        // todo: Should this be in here or in base SSO class?
        if( !getUserType().equals(ssoUser.getUserType().getName())) {
            WSUserType uType =  context.getUserManager().getUserType(getUserType());
            ssoUser.setUserType(uType);
        }

        /** Loop through requested workgroup names */
        List<String> newWorkGroups = getGroupDifference(ssoUser.getWorkgroups(), this.workGroups);

        if (newWorkGroups != null) {
            for (String workGroupName : getGroupDifference(ssoUser.getWorkgroups(), this.workGroups)) {

                /** Get the workgroup object by name*/
                WSWorkgroup workgroup = context.getUserManager().getWorkgroup(workGroupName);

                /** Add user to workgroup group */
                if (workgroup != null) {
                    ssoUser.addToGroup(workgroup);
                } else {
                    log.warn("Workgroup " + workGroupName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        /** Loop through requested locale names */
        List<String> newLocales = getGroupDifference(ssoUser.getLocales(), this.locales);

        if (newLocales != null) {

            for (String localeName : getGroupDifference(ssoUser.getLocales(), this.locales)) {

                /** Get the locale object by name*/
                WSLocale locale = context.getUserManager().getLocale(localeName);

                /** Add user to locale group */
                if (locale != null) {
                    ssoUser.addToGroup(locale);
                } else {
                    log.warn("Locale " + localeName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        /** Loop through requested workflow names */
        List<String> newWorkFlowRoles = getGroupDifference(ssoUser.getRoles(), this.workFlowRoles);

        if (newWorkFlowRoles != null) {
            for (String workFlowRoleName : getGroupDifference(ssoUser.getRoles(), this.workFlowRoles)) {

                /** Get the workflowrole object by name*/
                WSRole workFlowRole = context.getUserManager().getRole(workFlowRoleName);

                /** Add user to workFlowRole group */
                if (workFlowRole != null) {
                    ssoUser.addToGroup(workFlowRole);
                } else {
                    log.warn("WorkFlowRole " + workFlowRoleName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        /** Loop through requested client names */
        List<String> newClients = getGroupDifference(ssoUser.getClients(), this.clients);

        if (newClients != null) {
            for (String clientName : getGroupDifference(ssoUser.getClients(), this.clients)) {

                /** Get the client object by name*/
                WSClient client = context.getUserManager().getClient(clientName);

                /** Add user to client group */
                if (client != null) {
                    ssoUser.addToGroup(client);
                } else {
                    log.warn("Client " + clientName + " does not exist for user " + ssoUser.getUserName() + ".");
                }
            }
        }

        /** Loop through regional languages and see if any match requested */
        WSLanguage regionalSettingLanguage = null;

        for (WSLanguage language: context.getLinguisticManager().getLanguages()) {
            if(language.getName().equals(this.regionalSetting) &&
                    ssoUser.getRegionalSettingsLanguage().getName() != this.regionalSetting ) {
                regionalSettingLanguage = language;
            }
        }

        /** Loop through display languages and see if any match requested */
        WSLanguage UILanguage = null;
        for (WSLanguage language: context.getLinguisticManager().getAvailableDisplayLanguages()) {
            if(language.getName().equals(this.UIDisplayLanguage) &&
                    ssoUser.getDisplayLanguage().getName() != this.regionalSetting ) {
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
     * @param groups - An array of WorldServer groups
     * @param userGroups - An array of group names
     * @return - A list of strings containing any groups names not in the exsting groups
     */
    private List<String> getGroupDifference(WSGroup[] groups, String[] userGroups) {

        List<String> ret = new ArrayList<String>();
        int groupExists = 0;

        /** Loop through all groups requested */
        for( String userGroup: userGroups) {
            /** Loop through all existing groups */
            for( WSGroup group: groups ) {
                /** Check to see if they are equal */
                if(userGroup.equals(group.getName())) {
                    /** If they are then the group already exists */
                    groupExists++;
                }
            }

            /** If the group doesn't exist already then add it to the return list */
            if(groupExists == 0) {
                ret.add(userGroup);
            }
        }

        /** Return list of new groups to add */
        return ret;
    }
}
