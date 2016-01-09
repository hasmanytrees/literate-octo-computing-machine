package com.idiominc.ws.integration.compassion;

import com.idiominc.ws.integration.compassion.restService.RESTException;
import com.idiominc.ws.integration.compassion.restService.RESTService;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.asset.WSAssetTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.*;

/**
 * Created by bslack on 9/23/15.
 */
public class TranscriptionDataService {

    public static InputStream getImage(WSContext context, WSAssetTask task, int page, int dpi) throws IOException, RESTException {

        // Training environment update
        String letterURL = getLetterURL(task);
        if(letterURL != null && letterURL.startsWith("https://")) {
            // Standard server image; retrieve via ESB REST call
            return RESTService.getInstance(context).getImage(
                    context,
                    getLetterURL(task),
                    page,
                    dpi
            );
        } else {
            // Otherwise this should be a local test image path found in WorldServer AIS mount (/Customization/.../image)
            try {
                WSNode testNode = context.getAisManager().getNode(letterURL + page + ".jpg");
                if(testNode != null) {
                    return new FileInputStream(testNode.getFile());
                } else {
                    return null;
                }
            } catch (WSAisException e) {
                throw new IOException(e);
            }
        }

    }

    public static Document mergeTranscription(WSAssetTask task) {
        return mergeTranscription(task, false);
    }

    public static Document mergeTranscription(WSAssetTask task, boolean replaceSourceFile) {

        try {

            Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
            NodeList originalTextNodeList = XML.getNodes(xmlPayload, "//OriginalText");
            File[] working = WSAttributeUtils.getFileAttribute(null, task, "workingTranscription");
            if(working == null || working[0] == null) {
                // transcriptiton was not needed or not completed, as there is no data to merge;
                // optionally proceed without merging transcription text
                return xmlPayload;
            }

            JSONArray workingTranslationAsJSON = (JSONArray) JSONValue.parse(FileUtils.loadFileAsString(working[0], "UTF-8"));
            if (working != null && working.length != 0) {
                for (int x = 0; x < workingTranslationAsJSON.size(); x++) {
                    originalTextNodeList.item(x).setTextContent(workingTranslationAsJSON.get(x).toString());
                }
            }

            if (replaceSourceFile) {

                // when replacing file update English text to expose for translation
                NodeList englishTextNodeList = XML.getNodes(xmlPayload, "//EnglishTranslatedText");
                if (working != null && working.length != 0) {
                    for (int x = 0; x < workingTranslationAsJSON.size(); x++) {
                        englishTextNodeList.item(x).setTextContent(workingTranslationAsJSON.get(x).toString());
                    }
                }

                //re-write source file and reset fingerprint to prevent change flag
                String fingerPrint = task.getSourceAisNode().getFingerprint();
                Writer w = new OutputStreamWriter(task.getSourceAisNode().getOutputStream(), "UTF-8");
                XML.serialize(xmlPayload, w);
                FileUtils.close(w);
                task.getSourceAisNode().setProperty("segass_save_fingerprint", fingerPrint);
            }

            return xmlPayload;

        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }
    }

    private static String getLetterURL(WSAssetTask task) {
        try {
            Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
            return XML.getField(xmlPayload, "//OriginalLetterURL");
        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }
    }
}
