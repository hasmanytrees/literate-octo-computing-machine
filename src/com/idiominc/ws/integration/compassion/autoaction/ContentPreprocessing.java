package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticActionWithParameters;
import com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.asset.WSTextSegmentTranslation;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.linguistic.WSLanguage;
import com.idiominc.wssdk.td.WSTd;
import com.idiominc.wssdk.td.WSTdTerm;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.workflow.WSProject;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * Identify stop-words and questions from parsing source text.
 *
 */
public class ContentPreprocessing extends WSCustomTaskAutomaticActionWithParameters {

    //parameters
    private final String _ATTR_STOP_WORD_TD_NAME = "_ATTR_STOP_WORD_TD_NAME";
    private final String _ATTR_QUESTION_PUNCTUATION = "_ATTR_QUESTION_PUNCTUATION";

    //parameters to this automatic action
    private String stopWordTDNameStr;
    private String questionPunctuationStr;

    //version
    private String version = "1.0";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    //log
    private Logger log = Logger.getLogger(ContentPreprocessing.class);

    //variables with default values
    private String stopwordAttrName = "stopwordAttr";
    private String questionsFileAttrName = "questionFileAttachment";
    private String questionAttrName = "questionAttr";

    /**
     * Assign translation metadatra to projects and tasks
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //various metadata
        WSProject project = task.getProject();
        WSLocale sourceLocale = project.getSourceLocale();
        WSLanguage sourceLang = sourceLocale.getLanguage();
        WSLocale targetLocale = project.getTargetLocale();
        WSLanguage targetLang = targetLocale.getLanguage();

        //configuration files
        try {
            stopwordAttrName = Config.getStopWordsAttributeName(wsContext);
            questionsFileAttrName = Config.getQuestionsFileAttributeName(wsContext);
            questionAttrName = Config.getQuestionsAttributeName(wsContext);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        }

        WSTd stopwordTd = wsContext.getTdManager().getTd(stopWordTDNameStr);
        if(stopwordTd == null) {
            log.error("Could not find provided Stop word termbase named:" + stopWordTDNameStr);
            return new WSActionResult(WSActionResult.ERROR, "Could not find provided Stop word termbase named:" + stopWordTDNameStr);
        }
        StringBuilder stopwordBuffer = new StringBuilder();
        StringBuilder questionBuffer = new StringBuilder();

        // itereate through each text segment
        boolean stopwordFound = false;
        boolean questionFound = false;
        int segmentCounter = 1;
        for(Iterator textSegmentIt = task.getAssetTranslation().textSegmentIterator(); textSegmentIt.hasNext(); ) {
            WSTextSegmentTranslation segTrans = (WSTextSegmentTranslation)textSegmentIt.next();

            String srcText = segTrans.getSource();

            // first: search and identify any potential stop words
            WSTdTerm[][] foundTerms = stopwordTd.findTerms(srcText, sourceLang, targetLang);
            if(foundTerms != null && foundTerms.length > 0) {
                // stop words found!
                stopwordBuffer.append(segmentCounter).append("|");
                stopwordBuffer.append(srcText).append("|");
                for(WSTdTerm[] aTermArray : foundTerms) {
                    stopwordBuffer.append(aTermArray[0].getText()).append("|");
                    stopwordFound = true;
                }

                stopwordBuffer.append("|");
            }

            // second: look for any questions
            if(srcText != null && srcText.endsWith(questionPunctuationStr)) {
                //found a question! Add to buffer.
                questionBuffer.append(srcText).append("\n");
                questionFound = true;
            }

            segmentCounter++;
        }

        // if stopwords were found, set it
        if(stopwordFound) {
            task.setAttribute(stopwordAttrName, stopwordBuffer.toString());
        }

        // if questions were found, create the questions file
        if(questionFound) {
            // collect the questions and create a temporary file and attach to the project
            try {
                File tempQuestionFile = File.createTempFile("compassionLetter", "questions.txt");
                FileUtils.getStringAsFile(tempQuestionFile, questionBuffer.toString(), "UTF-8");
                task.getProject().setAttribute(questionsFileAttrName, tempQuestionFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Could not generate file to save Questions!", e);
                return new WSActionResult(WSActionResult.ERROR, "Could not generate file to output Questions found!");
            }

            // also save the content of the questions to the attribute for storing in the xml payload
            task.setAttribute(questionAttrName, questionBuffer.toString());
        }

        if(stopwordFound && questionFound) {
            return new WSActionResult(DONE_TRANSITION, "Completed processing the task and captured stop-words and questions.");
        } else if(stopwordFound) {
            return new WSActionResult(DONE_TRANSITION, "Completed processing the task and captured stop-words.");
        } else if(questionFound) {
            return new WSActionResult(DONE_TRANSITION, "Completed processing the task and captured questions.");
        } else {
            return new WSActionResult(DONE_TRANSITION, "Completed processing the task; did not find any stop-words and questions.");
        }

    }

    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {DONE_TRANSITION};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Preprocess Content for Stop-Words and Questions";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Process source segments and compare against term database to look for stop-words.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Extracts values of parameters to this AA
     * @param parameters - map of names / values of supplied parameters
     * @throws WSInvalidParameterException - exception
     */
    protected void preLoadParameters(Map parameters) throws WSInvalidParameterException {
        stopWordTDNameStr = preLoadParameter(parameters, _ATTR_STOP_WORD_TD_NAME, true);
        questionPunctuationStr = preLoadParameter(parameters, _ATTR_QUESTION_PUNCTUATION, true);
    }

    /**
     *
     * @return List of parameters expected by this AA
     */
    public WSParameter[] getParameters() {
        return new WSParameter[]{
                WSParameterFactory.createStringParameter(_ATTR_STOP_WORD_TD_NAME, "Stop Word Termbase Name", "Stop Word TD"),
                WSParameterFactory.createStringParameter(_ATTR_QUESTION_PUNCTUATION, "Question Punctuation Mark", "?")
        };
    }
}
