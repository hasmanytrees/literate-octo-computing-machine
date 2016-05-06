package com.idiominc.ws.integration.compassion.utilities;

import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.asset.WSAssetTask;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by cslack on 3/28/2016.
 */
public class MergeUtil {

    public static void copyPayloadTextFields(WSAssetTask task, WSNode aisNode, String originalField, String targetField) {

        try {

            Document xmlPayload = XML.load(aisNode.getFile());
            NodeList originalTextNodeList = XML.getNodes(xmlPayload, originalField);

            // when replacing file update English text to expose for translation
            NodeList targetTextNodeList = XML.getNodes(xmlPayload, targetField);

            for(int x = 0; x < originalTextNodeList.getLength(); x++) {
                targetTextNodeList.item(x).setTextContent(originalTextNodeList.item(x).getTextContent());
            }

            //re-write source file and reset fingerprint to prevent change flag
            String fingerPrint = aisNode.getFingerprint();
            Writer w = new OutputStreamWriter(aisNode.getOutputStream(), "UTF-8");
            XML.serialize(xmlPayload, w);
            FileUtils.close(w);
            aisNode.setProperty("segass_save_fingerprint", fingerPrint);

        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }
    }
}
