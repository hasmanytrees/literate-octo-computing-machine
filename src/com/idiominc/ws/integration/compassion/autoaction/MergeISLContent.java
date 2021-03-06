package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.ws.integration.compassion.utilities.MergeUtil;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Automatic action to copy and merge original text sent for ISL/electronic source content into English translated text field
 * in preparation for the first-step project creation.
 *
 * @author SDL Professional Services
 */
public class MergeISLContent extends WSCustomTaskAutomaticAction {

    private static final String _TRANSITION_DONE = "Done";

    /**
     * Update source English translated text field with Original text field received from ISL and prepare for ISL translation
     * @param context - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext context, WSAssetTask task) {

        try {

            Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
            NodeList originalTextNodeList = XML.getNodes(xmlPayload, "//OriginalText");

            // when replacing file update English text to expose for translation
            NodeList englishTextNodeList = XML.getNodes(xmlPayload, "//EnglishTranslatedText");

            for(int x = 0; x < originalTextNodeList.getLength(); x++) {
                englishTextNodeList.item(x).setTextContent(originalTextNodeList.item(x).getTextContent());
            }

            //re-write source file and reset fingerprint to prevent change flag
            String fingerPrint = task.getSourceAisNode().getFingerprint();
            Writer w = new OutputStreamWriter(task.getSourceAisNode().getOutputStream(), "UTF-8");
            XML.serialize(xmlPayload, w);
            FileUtils.close(w);
            task.getSourceAisNode().setProperty("segass_save_fingerprint", fingerPrint);


        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }

        return new WSActionResult(_TRANSITION_DONE, "Updated source content with ISL source text");
    }

    public String[] getReturns() {
        return new String[]{_TRANSITION_DONE};
    }

    public String getName() {
        return "Merge ISL Source Text";
    }

    public String getDescription() {
        return "Merges any ISL source content with the source asset";
    }
}
