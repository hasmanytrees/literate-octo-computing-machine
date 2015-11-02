package com.idiominc.ws.integration.compassion.servlet.UserAssignmentRuleUI;

import com.idiominc.ws.integration.compassion.servlet.Embed;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * Creates a WorldServer HTTP Servlet to provide an interface for assigning rules to workgroups to determine
 * what level of translator is needed for various complexity levels of documents
 *
 * Created by cslack on 10/20/2015.
 */

public class UserAssignmentRuleUI extends WSHttpServlet {

    private static final Logger log = Logger.getLogger(UserAssignmentRuleUI.class);

    public boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        WSHtmlContainer container = new WSHtmlContainer(context, request, response);

        try {

            // Load the core HTML, all CSS and JS is included from the HTML file
            Embed.embed(context, container, "userAssignmentRule/userAssignmentRule.html");
            container.printHtml(response.getWriter());

            return true;

        } catch (Exception e) {
            log.error("Could not embed HTML");
            throw new WSRuntimeException(e);
        }
    }


    /**
     * Returns the name of the WS customization
     *
     * @return WorldServer customization name
     */
    public String getName() {
        return "user_assignment_rule_ui";
    }


    /**
     * Returns the description of the WorldServer customization
     *
     * @return WorldServer customization description
     */
    public String getDescription() {
        return "The interface to manage user assignment rules.";
    }


    /**
     * Returns the version number of the WS customization
     *
     * @return WS customization version
     */
    public String getVersion() {
        return "1.0";
    }
}
