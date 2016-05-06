package com.idiominc.ws.integration.compassion.haservlet;

import com.idiominc.ws.integration.compassion.TranscriptionDataService;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.ws.integration.profserv.commons.wssdk.servlet.WSCustomHumanActionServletWithUI;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSSDKDialog;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectTranslationProgress;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Custom translation completion UI to check for empty/non-translated fields.
 */
public class TranslationCompleteUI extends WSCustomHumanActionServletWithUI {

    //logger
    private static final Logger log = Logger.getLogger(TranslationCompleteUI.class);

    //constants
    private static final String ESCALATE = "Escalate to Supervisor";
    private static final String RETURN_TO_QUEUE = "Return to Queue";
    private static final String TRANSLATION_COMPLETE = "Translation Completed";


    public boolean handleDefault(WSContext context, WSSDKDialog dialog, WSAssetTask[] tasks, HttpServletRequest request, HttpServletResponse response) {

        // Setup dialog box
        dialog.addHeader("<h1>Complete Translation</h1>");
        dialog.setCloseOnOK(true);
        dialog.setRefreshOnSubmit(true);

        // Include jQuery
        dialog.add("<script src=\"js/jquery/jquery.js\" id=\"jquery\"></script>");

        // Initialize with no empty fields

        boolean emptyFields = isTranslationEmpty(context, tasks);

        // If there are empty translation fields then disable the Translation Complete button
        if (emptyFields) {

            // Embed the JavaScript file that disables the Translate Complete button
            try {
                embedJS(context, dialog, "translateComplete/js/_nsSDL_translate_complete.js");
            } catch (WSAisException e) {
                log.error("Could not load _nsSDL_translate_complete.js from AIS Mount", e);
            } catch (IOException e) {
                log.error("Could not load _nsSDL_translate_complete.js", e);
            }
        }
        return true;
    }

    protected List validateInput(WSContext context, WSAssetTask[] tasks, HttpServletRequest request, HttpServletResponse response) {
        List<String> ret = new ArrayList<>();
        boolean emptyFields = isTranslationEmpty(context, tasks);
        if (emptyFields) {
            ret.add("Empty fields are not allowed!");
        }


        return ret;
    }


    public String[] getSupportedTransitionNames() {
        return new String[]{TRANSLATION_COMPLETE, ESCALATE, RETURN_TO_QUEUE};
    }

    public String getName() {
        return "Translation Completion UI";
    }

    public String getDescription() {
        return "Translation Completion UI";
    }

    public String getVersion() {
        return "1.0";
    }

    public static boolean isTranslationEmpty(WSContext context, WSAssetTask[] tasks) {

        Boolean emptyFields = false;

        WSProjectTranslationProgress translationProgress = tasks[0].getProject().getTranslationProgress();
        Boolean segmentedAsset = translationProgress.getTotalSegmentCount() > 0;

        // Check to see if there are any segments (if there aren't then it's likely a scanned letter)
        if (segmentedAsset) {

            /**
             * For now we will not be checking browser workbench translations, but may in the future, here is the code
             * to do so.
             */
//            // If there are segments then see if there are any that are untranslated
//            if(translationProgress.getUntranslatedSegmentCount() > 0) {
//
//                // If there are any untranslated segments then set emptyFields to true
//                emptyFields = true;
//            }

        } else {
            WSAssetTask lastTask = null;

            try {

                for (WSAssetTask task : tasks) {

                    lastTask = task;

                    Document xmlPayload = XML.load(task.getSourceAisNode().getFile());
                    NodeList englishTextNodeList = XML.getNodes(xmlPayload, "//EnglishTranslatedText");

                    int fieldCount = (englishTextNodeList == null ? 0 : englishTextNodeList.getLength());

                    if (fieldCount == 0) {
                        continue;
                    }

                    // If we've already determined that there are empty fields don't bother processing anymore
                    if (!emptyFields) {

                        // Get the working copy of the translation for this task
                        //  File[] working = WSAttributeUtils.getFileAttribute(null, task, "workingTranscription");

                        File workingFile = TranscriptionDataService.getTranscriptionWorkingFile(context, task);
                        // Check to see if there is a working transcription at all
                        if (workingFile == null) {

                            // transcription was not needed or not completed, as there is no data to merge;
                            // optionally proceed without merging transcription text
                            emptyFields = true;
                            break;
                        }

                        // If the working transcription does exist...

                        // Load the working transcription as JSON
                        JSONArray workingTranslationAsJSON = (JSONArray) JSONValue.parse(FileUtils.loadFileAsString(workingFile, "UTF-8"));

                        if (workingTranslationAsJSON == null || (englishTextNodeList.getLength() != workingTranslationAsJSON.size())) {
                            //todo: prefer to throw an exception? this shouldn't be possible
                            emptyFields = true;
                            break;
                        }


                        // Loop through all translation fields
                        for (int x = 0; !emptyFields && (x < workingTranslationAsJSON.size()); x++) {

                            // If any translation field is empty then indicate that we have empty fields
                            if (workingTranslationAsJSON.get(x).toString().equals("")) {
                                emptyFields = true;
                            }
                        }
                    }
                }
            } catch (IOException | WSAisException | SAXException | ParserConfigurationException | XPathException e) {
                log.error("Could not load XML for scanned letter." + getDetails(lastTask), e);
            }
        }

        return emptyFields;
    }

    private static String getDetails(WSAssetTask lastTask) {
        if (lastTask == null)
            return "Task not processed";

        return " [task:" + lastTask.getId() + " project:" + lastTask.getProject().getId() + " task#:" + lastTask.getTaskNumberInProject() + " path:" + lastTask.getSourcePath() + "]";
    }


    private void embedJS(WSContext context, WSSDKDialog hook, String name) throws WSAisException, IOException {
        hook.add("<script>\n");
        embed(context, hook, name, false);
        hook.add("\n</script>\n");
    }

    private void embed(WSContext context, WSSDKDialog hook, String name, boolean useVariables) throws WSAisException, IOException {
        WSNode transportCrateJS = context.getAisManager().getNode("/Customization/compassion_inc/" + name);
        Reader r = new InputStreamReader(transportCrateJS.getInputStream(), "UTF-8");
        String html = FileUtils.getReaderAsString(r);
        if (useVariables) {
            StringBuffer b = new StringBuffer();
            Matcher m = Pattern.compile("\\$([\\w.]+)").matcher(html);
            while (m.find()) {
                m.appendReplacement(b, dict.getString(m.group(1)));
            }
            m.appendTail(b);
            html = b.toString();
        }
        hook.add(html);
        FileUtils.close(r);
    }

    public String getUIToken() {
        return "compassion.ui.project_completion";
    }

}

