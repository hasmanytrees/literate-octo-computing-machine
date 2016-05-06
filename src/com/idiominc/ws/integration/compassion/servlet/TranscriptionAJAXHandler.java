package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.compassion.CompassionSecurity;
import com.idiominc.ws.integration.compassion.TranscriptionDataService;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.ws.integration.compassion.restService.RESTException;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import org.apache.axis.collections.LRUMap;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Supporting class to handle scanned letter editor AJAX requests
 *
 * @author SDL Professional Services
 */

public class TranscriptionAJAXHandler extends WSHttpServlet {


    public static void main(String[] args) {
        System.out.println("test");
    }

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
            response.setContentType("text/plain");

            try {

                log.error("CI API Error:" +
                        "\nMSG: " + e.getMessage() +
                        "\nCODE: " + e.getHttpCode() +
                        "\nTEXT: " + e.getResponseText() +
                        "\nLEN: " + e.getResponseLength() +
                        "");

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
            /*Throwable cause = e.getCause();

            while (cause != null) {
                log.error("Caused by: ", cause);
                cause = cause.getCause();
            }
            */

            response.setStatus(400);

            try {
                response.getWriter().write((e.getMessage() != null ? (e + ":" + e.getMessage()) : "UNKNOWN -- " + e));
            } catch (Exception e2) {
                log.info("Write error", e);
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

        return TranscriptionDataService.mergeTranscription(context, task);
    }


    private static final LRUMap tinyCache = new LRUMap(2);

    public Object command_getPageImage(WSContext context, HttpServletRequest request, HttpServletResponse response) throws CompassionSecurity.ACLException, IOException, RESTException {

        log.debug("GETPAGEIMAGE-TEST");
        int taskId = Integer.parseInt(request.getParameter("taskId"));
        int page = Integer.parseInt(request.getParameter("page"));
//        int dpi = Integer.parseInt(request.getParameter("dpi"));
        int width = Integer.parseInt(request.getParameter("width"));
        int quality = Integer.parseInt(request.getParameter("quality"));

        byte[] toReturn;

        long t1, t2;

        //TODO: ADDED FOR TRACKING AND DEBGGING 4/15/2015
//        String lruKey = "key_" + taskId + "$" + page + "$" + dpi;
        String lruKey = "key_" + taskId + "$" + page + "$" + width + "$" + quality;
        if (tinyCache.containsKey(lruKey)) {
            log.debug("HAS LRU CACHE IMAGE FOR TESTING; SKIP ESB!");
            log.debug("HAS LRU CACHE IMAGE FOR TESTING; SKIP ESB!");
            toReturn = (byte[]) tinyCache.get(lruKey);
        } else {

            WSAssetTask task = (WSAssetTask) context.getWorkflowManager().getTask(taskId);
            CompassionSecurity.check(context, CompassionSecurity.ACL.ACCESS_TASK, task);
            t1 = System.currentTimeMillis();
            InputStream imageStream = TranscriptionDataService.getImage(context, task, page, width, quality);
//            InputStream imageStream = TranscriptionDataService.getImage(context, task, page, dpi);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            proxyStream(request, response, imageStream, bos, "image/jpeg");
            t2 = System.currentTimeMillis();
            log.debug("" +
                    "FROM ESB:" +
                    "\n TASK: " + taskId + "" +
                    "\n PAGE: " + page + "" +
//                    "\n DPI : " + dpi + "" +
                    "\n WIDTH : " + width + "" +
                    "\n QUALITY : " + quality + "" +
                    "\n TOOK: " + (t2 - t1) + "ms" + "" +
                    "\n");

            toReturn = bos.toByteArray();
            tinyCache.put(lruKey, toReturn);
        }


        t1 = System.currentTimeMillis();
        response.setContentType("image/jpeg");
        response.getOutputStream().write(toReturn);
        t2 = System.currentTimeMillis();

        log.debug("" +
                "FROM WS:" +
                "\n TASK: " + taskId + "" +
                "\n PAGE: " + page + "" +
//                "\n DPI : " + dpi + "" +
                "\n WIDTH : " + width + "" +
                "\n QUALITY : " + quality + "" +
                "\n TOOK: " + (t2 - t1) + "ms" + "" +
                "\n");

        //TODO: ADDED FOR TRACKING AND DEBGGING 4/15/2015

        return null;
    }

    public Object command_setPageData(WSContext context, HttpServletRequest request, HttpServletResponse response) throws CompassionSecurity.ACLException, IOException {

        int taskId = Integer.parseInt(request.getParameter("taskId"));
        WSAssetTask task = (WSAssetTask) context.getWorkflowManager().getTask(taskId);
        CompassionSecurity.check(context, CompassionSecurity.ACL.ACCESS_TASK, task);

        //File workingData = File.createTempFile("working", ".transcription");
        String data = getBody(request);


        //workingData = FileUtils.getStringAsFile(workingData, data, "UTF-8");
        TranscriptionDataService.saveWorkingTranscription(context, task, data);

        //workingData.delete();
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

    protected Object proxyStream(HttpServletRequest request, HttpServletResponse response, InputStream is, OutputStream os, String contentType) throws IOException {
        response.setContentType(contentType);
        FileUtils.copyStream(is, os);
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
        return "1.0";
    }
}
