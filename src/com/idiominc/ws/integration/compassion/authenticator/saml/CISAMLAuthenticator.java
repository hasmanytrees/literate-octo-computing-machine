package com.idiominc.ws.integration.compassion.authenticator.saml;

import com.idiominc.ws.integration.profserv.commons.sso.SSOAuthenticator;
import com.idiominc.wssdk.WSAuthenticatorException;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSUser;

/**
 * Created by cslack on 9/11/2015.
 */
public class CISAMLAuthenticator extends SSOAuthenticator {

    public void authenticate(WSContext context, WSUser user, String password) throws WSAuthenticatorException {
        super.authenticate(context,user,password);
    }

    public String getName() {
        return "Compassion SAML Authenticator (1.0)";
    }
}
