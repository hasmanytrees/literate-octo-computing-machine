package com.idiominc.ws.integration.compassion;

import com.idiominc.ws.integration.compassion.restService.RESTException;
import com.idiominc.ws.integration.compassion.restService.RESTService;
import com.idiominc.ws.integration.compassion.utilities.CILetterType;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.workflow.WSTask;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by bslack on 9/23/15.
 */
public class TranscriptionDataService {

    private static final int _MAX_RETRY = 20;
    private static final int _SLEEP_TIME = 1000;

    private static final Logger log = Logger.getLogger(TranscriptionDataService.class);


//    public static InputStream getImage(WSContext context, WSAssetTask task, int page, int dpi) throws IOException, RESTException {
        public static InputStream getImage(WSContext context, WSAssetTask task, int page, int width, int quality) throws IOException, RESTException {

        // Training environment update
        String[] letterURLs = getLetterURL(task);
        if (letterURLs == null || letterURLs.length == 0) {
            throw new IOException("Not image data is present!");
        }

        if (letterURLs[0].startsWith("https://")) {
            // Standard server image; retrieve via ESB REST call

            String[] imageURLs = getLetterURL(task);
            return RESTService.getInstance(context).getImage(
                    context,
                    (CILetterType.getLetterType(task).isMultiPageImage() ? imageURLs[0] : imageURLs[page]),
                    (CILetterType.getLetterType(task).isMultiPageImage() ? page : -1),
//                    dpi
                    width,
                    quality
            );
        } else {
            // Otherwise this should be a local test image path found in WorldServer AIS mount (/Customization/.../image)
            try {
                WSNode testNode = null;

                if (CILetterType.getLetterType(task).isMultiPageImage()) {
                    testNode = context.getAisManager().getNode(letterURLs[0] + page + ".jpg");
                } else {
                    testNode = context.getAisManager().getNode(letterURLs[page]);
                }
                if (testNode != null) {
                    return new FileInputStream(testNode.getFile());
                } else {
                    return null;
                }
            } catch (WSAisException e) {
                throw new IOException(e);
            }
        }

    }

    public static Document mergeTranscription(WSContext context, WSAssetTask task) {
        return mergeTranscription(context, task, false);
    }

    public static Document mergeTranscription(WSContext context, WSAssetTask task, boolean replaceSourceFile) {

        try {

            Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
            NodeList originalTextNodeList = XML.getNodes(xmlPayload, "//OriginalText");
            // File[] working = WSAttributeUtils.getFileAttribute(null, task, "workingTranscription");
            File workingFile = getTranscriptionWorkingFile(context, task);

            if (workingFile == null || !workingFile.exists() || workingFile.length() == 0) {
                // transcriptiton was not needed or not completed, as there is no data to merge;
                // optionally proceed without merging transcription text
                return xmlPayload;
            }


            JSONArray workingTranslationAsJSON = (JSONArray) JSONValue.parse(FileUtils.loadFileAsString(workingFile, "UTF-8"));

            for (int x = 0; x < workingTranslationAsJSON.size(); x++) {
                originalTextNodeList.item(x).setTextContent(workingTranslationAsJSON.get(x).toString());
            }

            if (replaceSourceFile) {

                // when replacing file update English text to expose for translation
                NodeList englishTextNodeList = XML.getNodes(xmlPayload, "//EnglishTranslatedText");
                for (int x = 0; x < workingTranslationAsJSON.size(); x++) {
                    englishTextNodeList.item(x).setTextContent(workingTranslationAsJSON.get(x).toString());
                }

                //re-write source file and reset fingerprint to prevent change flag
                String fingerPrint = task.getSourceAisNode().getFingerprint();
                Writer w = new OutputStreamWriter(task.getSourceAisNode().getOutputStream(), "UTF-8");
                XML.serialize(xmlPayload, w);
                FileUtils.close(w);
                task.getSourceAisNode().setProperty("segass_save_fingerprint", fingerPrint);


                if (!workingFile.delete()) {
                    log.error("Delete failed for working file:" + workingFile.getAbsolutePath());
                }
            }

            return xmlPayload;

        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }
    }

    public static synchronized void saveWorkingTranscription(WSContext context, WSTask task, String workingData) throws IOException {

        //   WSAttributeUtils.setFileAttribute(this, task, "workingTranscription", new File[]{workingData});

        try {
            WSNode workingNode = getTranscriptionWorkingNode(context, (WSAssetTask) task, true);

            Reader reader = null;
            Writer writer = null;

            try {
                reader = new StringReader(workingData);
                writer = new OutputStreamWriter(workingNode.getOutputStream(), "UTF-8");
            } finally {
                FileUtils.copyReader(reader, writer);
                FileUtils.close(reader);
                FileUtils.close(writer);
            }

        } catch (WSAisException e) {
            throw new IOException(e);
        }


    }

    public static File getTranscriptionWorkingFile(WSContext context, WSAssetTask task) throws WSAisException {
        WSNode workingNode = getTranscriptionWorkingNode(context, task);
        if (workingNode == null) {
            File[] working = WSAttributeUtils.getFileAttribute(null, task, "workingTranscription");
            if (working != null && working.length > 0) {
                return working[0];
            } else {
                return null;
            }
        }

        return workingNode.getFile();
    }

    public static WSNode getTranscriptionWorkingNode(WSContext context, WSAssetTask task) throws WSAisException {
        return getTranscriptionWorkingNode(context, task, false);
    }

    public static WSNode getTranscriptionWorkingNode(WSContext context, WSAssetTask task, boolean create) throws WSAisException {
        String workingPath = task.getTargetPath() + ".transcription";
        WSNode node = context.getAisManager().getNode(workingPath);
        if (create) {
            if (node == null) {
                node = context.getAisManager().create(workingPath, task.getSourceAisNode());
            }

            if (node == null) {
                throw new WSAisException("Could not create working node:" + workingPath);
            }
        }

        return node;
    }


    private static String[] getLetterURL(WSAssetTask task) {
        try {
            Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
            String[] letterURLs = XML.getFields(xmlPayload, "//OriginalLetterURL/text()");
            System.out.println(letterURLs.length);
            System.out.println(Arrays.asList(letterURLs));
            return letterURLs;
        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }
    }


}
