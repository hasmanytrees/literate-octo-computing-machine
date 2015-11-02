package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.compassion.CompassionSecurity;
import com.idiominc.ws.integration.compassion.TranscriptionDataService;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.ws.integration.compassion.restService.RESTException;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bslack on 9/18/15.
 */
public class TranscriptionAJAXHandler extends WSHttpServlet {

    private static final Logger log = Logger.getLogger(TranscriptionAJAXHandler.class);

    public boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {
        // because we do things like changing cost model on-the-fly, we often DON'T want to commit !
        boolean commit = false;

        try {
            String command = request.getParameter("command");
            String mimeType = request.getParameter("contentType");


            if (command.startsWith("get")) commit = false;
            if (command.startsWith("set")) commit = true;

            response.setContentType((mimeType == null ? "application/json" : mimeType) + "; charset=utf-8");
            response.setStatus(200);

            Object result = null;
            if ("getCheckAccess".equals(command)) {
                result = command_getCheckAccess(context, request, response);
            } else if ("getPageData".equals(command)) {
                result = command_getPageData(context, request, response);
            } else if ("getPageImage".equals(command)) {
                result = command_getPageImage(context, request, response);
            } else if ("setPageData".equals(command)) {
                result = command_setPageData(context, request, response);
            }

            if (result == null)
                return commit;


            if (mimeType != null && mimeType.endsWith("xml")) {

                if (result instanceof Document) {
                    response.getWriter().write(XML.serializeNode((Document) result));
                } else {
                    response.getWriter().write(result.toString());
                }

            } else if (result instanceof File) {
                File file = (File) result;
                if ("yes".equals(request.getParameter("download"))) {
                    asDownload(request, response, file);
                } else {
                    JSONObject.writeJSONString(asMetadata(request, file), response.getWriter());
                }
            } else {
                JSONValue.writeJSONString(result, response.getWriter());
            }

            response.getWriter().flush();
            response.flushBuffer();

            return commit;

        } catch (CompassionSecurity.ACLException e) {
            response.setStatus(400);

            log.warn("Vendor API ACL violation: " + e.getMessage());
            try {
                response.getWriter().write(e.getMessage());
            } catch (Exception e2) {
                log.error("ACL ERROR", e);
            }

            return false;
        } catch (RESTException e) {

            log.error("CI API Error", e);
            response.setStatus(e.getHttpCode());

            try {
                response.getWriter().write("CI API Error: \n" +
                        "\nMSG: " + e.getMessage() +
                        "\nCODE: " + e.getHttpCode() +
                        "\nTEXT: " + e.getResponseText() +
                        "\nLEN: " + e.getResponseLength() +
                        "");


            } catch (Exception e2) {
                log.error("Writer error", e);
            }

            return false;


        } catch (Exception e) {

            log.error("General error", e);
            response.setStatus(400);

            try {
                response.getWriter().write((e.getMessage() != null ? (e + ":" + e.getMessage()) : "UNKNOWN -- " + e));
            } catch (Exception e2) {
                log.error("Write error", e);
            }

            return false;
        }
    }

    public Object command_getCheckAccess(WSContext context, HttpServletRequest request, HttpServletResponse response) throws CompassionSecurity.ACLException, IOException {

        int taskId = Integer.parseInt(request.getParameter("taskId"));
        WSAssetTask task = (WSAssetTask) context.getWorkflowManager().getTask(taskId);
        CompassionSecurity.check(context, CompassionSecurity.ACL.ACCESS_TASK, task);
        return true;

    }

    public Object command_getPageData(WSContext context, HttpServletRequest request, HttpServletResponse response) throws CompassionSecurity.ACLException, IOException {

        int taskId = Integer.parseInt(request.getParameter("taskId"));
        WSAssetTask task = (WSAssetTask) context.getWorkflowManager().getTask(taskId);
        CompassionSecurity.check(context, CompassionSecurity.ACL.ACCESS_TASK, task);

        return TranscriptionDataService.mergeTranscription(task);
    }

    public Object command_getPageImage(WSContext context, HttpServletRequest request, HttpServletResponse response) throws CompassionSecurity.ACLException, IOException, RESTException {

        int taskId = Integer.parseInt(request.getParameter("taskId"));
        int page = Integer.parseInt(request.getParameter("page"));
        int dpi = Integer.parseInt(request.getParameter("dpi"));

        WSAssetTask task = (WSAssetTask) context.getWorkflowManager().getTask(taskId);
        CompassionSecurity.check(context, CompassionSecurity.ACL.ACCESS_TASK, task);
        InputStream imageStream = TranscriptionDataService.getImage(context, task, page, dpi);
        proxyStream(request, response, imageStream, "image/jpeg");

        return null;

    }

    public Object command_setPageData(WSContext context, HttpServletRequest request, HttpServletResponse response) throws CompassionSecurity.ACLException, IOException {
        int taskId = Integer.parseInt(request.getParameter("taskId"));
        WSAssetTask task = (WSAssetTask) context.getWorkflowManager().getTask(taskId);
        CompassionSecurity.check(context, CompassionSecurity.ACL.ACCESS_TASK, task);

        File workingData = File.createTempFile("working", ".transcription");
        String data = getBody(request);

        workingData = FileUtils.getStringAsFile(workingData, data, "UTF-8");
        WSAttributeUtils.setFileAttribute(this, task, "workingTranscription", new File[]{workingData});
        workingData.delete();
        return "OK";
    }

    private String getBody(HttpServletRequest request) throws IOException {

        StringBuffer jb = new StringBuffer();
        String line = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
        while ((line = reader.readLine()) != null) {
            jb.append(line);
        }

        return jb.toString();
    }

    private Object proxyStream(HttpServletRequest request, HttpServletResponse response, InputStream is, String contentType) throws IOException {
        response.setContentType(contentType);
        FileUtils.copyStream(is, response.getOutputStream());
        FileUtils.close(is);
        return null;
    }

    private void asDownload(HttpServletRequest request, HttpServletResponse response, File file) throws IOException {

        if (file.getName().matches(".*jpe?g")) {
            response.setContentType("image/jpeg");
        } else if (file.getName().matches(".*png")) {
            response.setContentType("image/png");
        } else {
            String fn = file.getName().substring(file.getName().indexOf("_") + 1);
            if (fn.startsWith("upload_")) fn = fn.substring(fn.indexOf(".") + 1);
            response.setContentType("application/octet");
            response.setHeader("Content-Disposition", "file;filename=\"" + fn + "\"");
        }

        FileUtils.fileToStream(file, response.getOutputStream());

    }

    private Map<String, String> asMetadata(HttpServletRequest request, File file) {
        return asMetadata(request, file, null);
    }

    private Map<String, String> asMetadata(HttpServletRequest request, File file, String command) {
        String queryString = request.getQueryString();
        if (command != null) {
            queryString = queryString.replaceFirst("(.*command=)(.*?)(&)", "$1" + command + "$3");
        }
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("name", file.getName().substring(file.getName().indexOf("_") + 1));
        ret.put("link", request.getContextPath() + "/" + request.getServletPath() + "?" + queryString + "&download=yes");
        return ret;
    }


    public String getName() {
        return "ajax_transcription_api";
    }

    public String getDescription() {
        return "AJAX API for the transcription client";
    }

    public String getVersion() {
        return "0.1-test";
    }
}
