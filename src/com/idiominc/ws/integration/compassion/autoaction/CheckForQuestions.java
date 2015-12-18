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
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.workflow.WSProject;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dkoo on 12/9/2015.
 */
public class CheckForQuestions extends WSCustomTaskAutomaticActionWithParameters {

    //parameters
    private final String _ATTR_QUESTION_PUNCTUATION = "_ATTR_QUESTION_PUNCTUATION";

    //parameters to this automatic action
    private String questionPunctuationStr;

    //version
    private String version = "1.0";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    //log
    private Logger log = Logger.getLogger(CheckForQuestions.class);

    //variables with default values
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
            questionsFileAttrName = Config.getQuestionsFileAttributeName(wsContext);
            questionAttrName = Config.getQuestionsAttributeName(wsContext);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        }

        StringBuilder questionBuffer = new StringBuilder();

        // itereate through each text segment
        boolean questionFound = false;
        for(Iterator textSegmentIt = task.getAssetTranslation().textSegmentIterator(); textSegmentIt.hasNext(); ) {
            WSTextSegmentTranslation segTrans = (WSTextSegmentTranslation)textSegmentIt.next();

            String text = segTrans.getTarget();
            if(text == null || text.equals("")) {
                text = segTrans.getSource();
            }

            // look for any questions
            if(text != null && text.endsWith(questionPunctuationStr)) {
                //found a question! Add to buffer.
                questionBuffer.append(text).append("\n");
                questionFound = true;
            }
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

        if(questionFound) {
            return new WSActionResult(DONE_TRANSITION, "Completed post-processing the task and captured questions.");
        } else {
            return new WSActionResult(DONE_TRANSITION, "Completed post-processing the task; did not find any questions.");
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
        return "Post-Process Content for Translated Questions";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Process target segments for questions.";
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
        questionPunctuationStr = preLoadParameter(parameters, _ATTR_QUESTION_PUNCTUATION, true);
    }

    /**
     *
     * @return List of parameters expected by this AA
     */
    public WSParameter[] getParameters() {
        return new WSParameter[]{
                WSParameterFactory.createStringParameter(_ATTR_QUESTION_PUNCTUATION, "Question Punctuation Mark", "?")
        };
    }
}

