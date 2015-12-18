package com.idiominc.ws.integration.compassion.authenticator.saml;


import com.idiominc.ws.integration.profserv.commons.sso.saml.SAMLAttribute;
import com.idiominc.ws.integration.profserv.commons.sso.saml.SAMLAuthorization;
import com.idiominc.wssdk.WSContext;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Created by cslack on 9/11/2015.
 */
public class CISAMLAuthorization extends SAMLAuthorization {

    private static final Logger log = Logger.getLogger(CISAMLAuthorization.class);

    private static final String _CI_REDIRECT = "https://globalaccess.ci.org/";

    public CISSOUser authenticate(WSContext context, HttpServletRequest request) throws IOException {

        try {

            Map<String, String> samlVariables = getVariables(context, request,
                    new SAMLAttribute("email", "email"),
                    new SAMLAttribute("firstname", "firstname"),
                    new SAMLAttribute("lastname", "lastname"),
                    new SAMLAttribute("username", "username"),
                    new SAMLAttribute("usertype", "usertype"),
                    new SAMLAttribute("workgroups", "workgroups"),
                    new SAMLAttribute("locales", "locales"),
                    new SAMLAttribute("workflowroles", "workflowroles"),
                    new SAMLAttribute("UIDisplayLanguage", "UIDisplayLanguage"),
                    new SAMLAttribute("regionalSetting", "regionalSetting"),
                    new SAMLAttribute("clients", "clients")
            );

            CISSOUser ssoUser = new CISSOUser(
                    samlVariables.get("username"), // user id
                    samlVariables.get("username"), // user name
                    samlVariables.get("email"),
                    samlVariables.get("firstname"),
                    samlVariables.get("lastname"),
                    samlVariables.get("usertype"),
                    samlVariables.get("workgroups"),
                    samlVariables.get("locales"),
                    samlVariables.get("workflowroles"),
                    samlVariables.get("UIDisplayLanguage"),
                    samlVariables.get("regionalSetting"),
                    samlVariables.get("clients")
            );

            return ssoUser;

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /** todo: this needs to be replaced with Compassion's info */
    /** This is the link location the the "CI Login" button on the login screen goes to */
    public String getRedirectTo() {
        return _CI_REDIRECT;
    }
}
