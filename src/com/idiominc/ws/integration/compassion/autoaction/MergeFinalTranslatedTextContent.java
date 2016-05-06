package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.ws.integration.compassion.haservlet.TranslationCompleteUI;
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
 * Copy over English translated text into Translated text fields in source document for further translation (second-step).
 *
 * @author SDL Professional Services
 */
public class MergeFinalTranslatedTextContent extends WSCustomTaskAutomaticAction {

    private static final String _TRANSITION_DONE = "Done";

    /**
     * Update source Translated text field with English translated field and prepare for second step translation
     *
     * @param context - WorldServer Context
     * @param task    - project's task
     */
    public WSActionResult execute(WSContext context, WSAssetTask task) {

        try {

//            boolean emptyFields = TranslationCompleteUI.isTranslationEmpty(context, new WSAssetTask[]{task});
//            if (emptyFields) {
//                return new WSActionResult(WSActionResult.ERROR, "Translation contains illegal empty fields. Cannot proceed!");
//            }

            Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
            NodeList originalTextNodeList = XML.getNodes(xmlPayload, "//EnglishTranslatedText");

            // when replacing file update English text to expose for translation
            NodeList englishTextNodeList = XML.getNodes(xmlPayload, "//TranslatedText");

//            if (englishTextNodeList == null || englishTextNodeList.getLength() == 0) {
//                return new WSActionResult(_TRANSITION_DONE, "The source file contains no translatable content");
//            }

            for (int x = 0; x < originalTextNodeList.getLength(); x++) {
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

        return new WSActionResult(_TRANSITION_DONE, "Updated Translated text with English translated content");
    }

    public String[] getReturns() {
        return new String[]{_TRANSITION_DONE};
    }

    public String getName() {
        return "Merge Translated Text";
    }

    public String getDescription() {
        return "Merges any Translated text content with the English translated content";
    }
}
