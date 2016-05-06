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
 * Merge and copy over English translated text from Target asset into Translated text field for delivery prep.
 *
 * @author SDL Professional Services
 */
public class MergeFinalTranslatedTextContentTarget extends WSCustomTaskAutomaticAction {

    private static final String _TRANSITION_DONE = "Done";

    /**
     * Update target Translated text field with English translated field and prepare for second step translation
     * @param context - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext context, WSAssetTask task) {

        try {

            Document xmlPayload = XML.load(task.getTargetAisNode().getFile());
            NodeList originalTextNodeList = XML.getNodes(xmlPayload, "//EnglishTranslatedText");

            // when replacing file update English text to expose for translation
            NodeList englishTextNodeList = XML.getNodes(xmlPayload, "//TranslatedText");

            for(int x = 0; x < originalTextNodeList.getLength(); x++) {
                englishTextNodeList.item(x).setTextContent(originalTextNodeList.item(x).getTextContent());
            }

            //re-write source file and reset fingerprint to prevent change flag
            String fingerPrint = task.getTargetAisNode().getFingerprint();
            Writer w = new OutputStreamWriter(task.getTargetAisNode().getOutputStream(), "UTF-8");
            XML.serialize(xmlPayload, w);
            FileUtils.close(w);
            task.getTargetAisNode().setProperty("segass_save_fingerprint", fingerPrint);


        } catch (Exception e) {
            throw new com.idiominc.wssdk.WSRuntimeException(e);
        }

        return new WSActionResult(_TRANSITION_DONE, "Updated target Translated text with English translated content");
    }

    public String[] getReturns() {
        return new String[]{_TRANSITION_DONE};
    }

    public String getName() {
        return "Merge Translated Text-Target";
    }

    public String getDescription() { return "Merges target Translated text content with the English translated content"; }
}
