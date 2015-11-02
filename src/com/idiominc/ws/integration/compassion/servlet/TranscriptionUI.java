package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by bslack on 9/23/15.
 */
public class TranscriptionUI extends WSHttpServlet {

    public boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        WSHtmlContainer container = new WSHtmlContainer(context, request, response);

        try {
            Embed.embed(context, container, "transcription/viewer.html");
            container.printHtml(response.getWriter());
            return true;
        } catch (Exception e) {//todo
            throw new WSRuntimeException(e);
        }
    }

    public String getName() {
        return "transcription_ui";
    }

    public String getDescription() {
        return "The interface to transcribe image content prior to or in place of translation";
    }

    public String getVersion() {
        return "1.0";
    }
}
