package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to launch the transcription/scanned letter editor HTML UI (and included JS files)
 *
 * @author SDL Professional Services
 */
public class ImageViewerUI extends WSHttpServlet {

    private static final Logger log = Logger.getLogger(TranscriptionUI.class);

    public boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        WSHtmlContainer container = new WSHtmlContainer(context, request, response);

        try {
            Embed.embed(context, container, "imageviewer/viewer.html");
            container.printHtml(response.getWriter());
            return true;
        } catch (Exception e) {//todo
            log.error(e.getMessage(), e);
            throw new WSRuntimeException(e);
        }
    }

    public String getName() {
        return "imageviewer_ui";
    }

    public String getDescription() {
        return "The interface to view embedded image references during translation";
    }

    public String getVersion() {
        return "1.0";
    }
}
