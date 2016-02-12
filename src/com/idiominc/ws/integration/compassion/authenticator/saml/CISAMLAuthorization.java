package com.idiominc.ws.integration.compassion.authenticator.saml;


import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.profserv.commons.sso.saml.SAMLAttribute;
import com.idiominc.ws.integration.profserv.commons.sso.saml.SAMLAuthorization;
import com.idiominc.wssdk.WSContext;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Compassion SAML authorization extending common SAML authorization class to provide standard SAML support.
 *
 * @author SDL Professional Services
 */
public class CISAMLAuthorization extends SAMLAuthorization {

    private static final Logger log = Logger.getLogger(CISAMLAuthorization.class);

    // Default value;  will be overriden with value from configuration file
    private static final String _CI_REDIRECT = "https://globalaccess.ci.org/";

    /**
     * Perform user authentication based on provided input
     *
     * @param context WorldServer context
     * @param request HTTP servlet request
     * @return Authenticated CI SSO User object
     * @throws IOException Exception from getting authenticated SSO user
     */
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

    /** This is the link location the the "CI Login" button on the login screen goes to */
    public String getRedirectTo(WSContext context) {

        String url = _CI_REDIRECT;

        try {
            url = Config.getSSOURL(context);
        } catch(IOException e) {
            log.error("No sso.url found in custom.properties file.");
        }

        return url;
    }
}
