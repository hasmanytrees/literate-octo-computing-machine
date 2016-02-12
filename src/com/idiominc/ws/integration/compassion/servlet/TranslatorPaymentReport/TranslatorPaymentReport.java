package com.idiominc.ws.integration.compassion.servlet.TranslatorPaymentReport;

import com.idiominc.ws.integration.compassion.servlet.Embed;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRuntimeException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Main servlet for the Translator Payment Report
 *
 * Created by cslack on 1/25/2016.
 */
public class TranslatorPaymentReport extends WSHttpServlet {

    private static final Logger log = Logger.getLogger(TranslatorPaymentReport.class);

    public boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        WSHtmlContainer container = new WSHtmlContainer(context, request, response);

        try {
            // Load the HTML File
            Embed.embed(context, container, "translatorPaymentReport/translatorPaymentReport.html");

            // Display HTML
            container.printHtml(response.getWriter());

            return true;

        } catch (Exception e) {
            log.error("Could not embed HTML.");
            throw new WSRuntimeException(e);
        }
    }


    /**
     * Returns the name of the WS customization
     *
     * @return WorldServer customization name
     */
    public String getName() {
        return "translator_payment_report";
    }


    /**
     * Returns the description of the WorldServer customization
     *
     * @return WorldServer customization description
     */
    public String getDescription() {
        return "Generates translator payment report.";
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
