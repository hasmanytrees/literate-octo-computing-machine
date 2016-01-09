package com.idiominc.ws.integration.compassion.servlet.TranslatorRatingUI;

import com.idiominc.ws.integration.compassion.servlet.Embed;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * UI class for the Translator Rating Editor
 *
 * @author SDL Professional Services
 */
public class TranslatorRatingUI extends WSHttpServlet {

    private static final Logger log = Logger.getLogger(TranslatorRatingUI.class);

    public boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        WSHtmlContainer container = new WSHtmlContainer(context, request, response);

        try {

            // Load the core HTML, all CSS and JS is included from the HTML file
            Embed.embed(context, container, "translatorRating/translatorRating.html");

            // Allow HTML to be displayed
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
        return "translator_rating_ui";
    }


    /**
     * Returns the description of the WorldServer customization
     *
     * @return WorldServer customization description
     */
    public String getDescription() {
        return "The interface to manage translator ratings.";
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
