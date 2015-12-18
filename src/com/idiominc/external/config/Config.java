package com.idiominc.external.config;

//log4j

import org.apache.log4j.Logger;

//java
import java.io.IOException;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;

//profserv
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSUtils;


/**
 * Configuration file for custom.properties file parsing and maintenance
 * File is stored under the /Customizations mount
 */
public class Config {

    //constants

    //user object's rating attribute name - contains XML structure for parsing
    public final static String _USER_RATING_ATTRIBUTE = "rating";

    //complexity atribute name
    public final static String _PROJECT_COMPLEXITY_ATTRIBUTE = "TranslationComplexity";

    //configuration file handling
    private static long _lastModified = 0;

    //constants
    private static Properties _config = new Properties();

    //logger
    private static Logger _log = Logger.getLogger(Config.class);

    //flag
    private static boolean _initiated = false;

    //data
    private static String _translatorWorkflowRole = null; //translator role
    private static String _supervisorWorkflowRole = null; //supervisor workflow role
    private static String _assignmentRuleConfigurationFileName = null; //the localtion and name of the global file that holds complexity rules

    private static String _translateStepName = null; //Translate
    private static String _translationQueueStepName = null; //Translation Queue
    private static String _performQCStepName = null; //Perform QC
    private static String _QCQueueStepName = null; //QC Queue
    private static String _updateTranslationStepName = null; //Update Translation
    private static String _updateTranslationQueueStepName = null; //Update Translation Queue
    private static String _translationRejectedAttributeName = null;
    private static String _translatorsCountAttributeName = null;
    private static String _listOfStopWordsAttributeName = null;
    private static String _listOfQuestionsAttributeName = null;
    private static String _questionsFileAttributeName = null;
    private static String _qcQualifiedAttributeName = null;
    private static String _translationsIterationCountAttributeName = null;

    private static String _restApiClient = null;
    private static String _restApiSecret = null;
    private static String _restApiAPIkey = null;
    private static String _restApiBaseURL = null;
    private static String _restApiBaseCommand = null;
    private static String _restApiESBStatusCommand = null;
    private static String _restApiOAuthToken = null;


    private static String _apiNtlmUser = null;
    private static String _apiIntermediaryLocale = null;
    private static String _apiWorkflowNameISL = null;
    private static String _apiWorkflowNameTranslation = null;
    private static String _apiWorkflowNameTranscription = null;

    /**
     * Reinitialize properties file if it got changed
     *
     * @param context - WS context
     * @throws Exception - exception (IO)
     */
    private static void refresh(WSContext context) throws IOException {
        File f = getConfigFile(context);
        if (f.lastModified() != _lastModified) {
            init(context);
        }
    }

    /**
     * Get the handle to this properties file
     *
     * @param context - WS context
     * @return - File handle to the file
     * @throws Exception - IO exception
     */
    private static File getConfigFile(WSContext context) throws IOException {
        try {
            File f = new File(WSUtils.getConfigNode(context).getFile(), "custom.properties");
            if (!f.exists()) {
                throw new WSAisException("Can't locate configuraiton file " +
                        f.getAbsolutePath());
            }
            return f;
        } catch (WSAisException e) {
            throw new IOException(e);
        }
    }

    /**
     * Initialize this properties file and load it
     *
     * @param context - WS context
     * @throws Exception - IO exception
     */
    private static void init(WSContext context) throws IOException {
        _initiated = true;
        try {
            File f = getConfigFile(context);
            _lastModified = f.lastModified();
            FileInputStream fis = new FileInputStream(f);
            _config.load(fis);
            FileUtils.close(fis);
        } catch (Exception e) {
            _log.error(e.getLocalizedMessage());
        }
        _translatorWorkflowRole = safeGetString("workflow_role_name.translator", "Translators");
        _supervisorWorkflowRole = safeGetString("workflow_role_name.supervisor", "Supervisors");
        _assignmentRuleConfigurationFileName = safeGetString("configfile.assignmentRule", "/Customizations/AssignmentRules.xml");
        _translateStepName = safeGetString("step_name.translate", "Translate");
        _translationQueueStepName = safeGetString("step_name.translation_queue", "Translation Queue");
        _performQCStepName = safeGetString("step_name.perform_qc", "Perform QC");
        _QCQueueStepName = safeGetString("step_name.qc_queue", "QC Queue");
        _updateTranslationStepName = safeGetString("step_name.update_translation", "Update Translation");
        _updateTranslationQueueStepName = safeGetString("step_name.update_translation_queue", "Update Translation Queue");
        _translationRejectedAttributeName = safeGetString("attr.translation_rejected", "qcResult");
        _translatorsCountAttributeName = safeGetString("attr.translations_count", "trCount");
        _listOfStopWordsAttributeName = safeGetString("attr.listOfStopWords", "stopwordAttr");
        _listOfQuestionsAttributeName = safeGetString("attr.listOfQuestions", "questionAttr");
        _questionsFileAttributeName = safeGetString("attr.questionsFile", "questionFileAttachment");
        _qcQualifiedAttributeName = safeGetString("attr.qcQualified", "qcQualified");
        _translationsIterationCountAttributeName = safeGetString("attr.translationsIteration_count", "trIterationCount");

        _restApiClient = safeGetString("restApi.client", "");
        _restApiSecret = safeGetString("restApi.secret", "");
        _restApiAPIkey = safeGetString("restApi.apiKey", "");
        _restApiBaseURL = safeGetString("restApi.baseURL", "https://api2.compassion.com/");
        _restApiBaseCommand = safeGetString("restApi.baseCommand", "");
        _restApiESBStatusCommand = safeGetString("restApi.esbStatusCommand", "");
        _restApiOAuthToken = safeGetString("restApi.oAuthToken", "");


        _apiNtlmUser = safeGetString("api.ntlmUser", "GLOBAL\\bslack");
        _apiIntermediaryLocale = safeGetString("api.intermediaryLocale", "English");
        _apiWorkflowNameISL = safeGetString("api.workflowNameISL", "Compassion ISL Translation and Review Workflow-TwoStep - Step 1");
        _apiWorkflowNameTranslation = safeGetString("api.workflowNameTranslation", "Compassion Translation and Review Workflow-TwoStep - Step 1");
        _apiWorkflowNameTranscription = safeGetString("api.workflowNameTranscription", "Compassion Transcription, Translation and Review Workflow-TwoStep - Step 1");

    }

    public static String getNTLMUser(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _apiNtlmUser;
    }


    public static String getIntermediaryLocale(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _apiIntermediaryLocale;
    }


    public static String getWorkflowNameISL(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _apiWorkflowNameISL;
    }

    public static String getWorkflowNameTranslation(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _apiWorkflowNameTranslation;
    }

    public static String getWorkflowNameTranscription(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _apiWorkflowNameTranscription;
    }


    public static String getRESTApiClient(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiClient;
    }

    public static String restRESTApiSecret(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiSecret;
    }

    public static String getRESTApiKey(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiAPIkey;
    }

    /***
     private static String _restApiOAuthToken = null;
     */

    public static String getRESTApiBaseCommand(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiBaseCommand;
    }

    public static String getRESTApiESBStatusCommand(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiESBStatusCommand;
    }

    public static String getRESTApiOAuthToken(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiOAuthToken;
    }

    public static String getRESTApiBaseURL(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _restApiBaseURL;
    }

    /**
     * Obtain attribute name for storing stop words in the task
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getQCQualifiedAttributeName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _qcQualifiedAttributeName;
    }


    /**
     * Obtain attribute name for storing stop words in the task
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getStopWordsAttributeName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _listOfStopWordsAttributeName;
    }

    /**
     * Obtain attribute name for storing questions in the task
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getQuestionsAttributeName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _listOfQuestionsAttributeName;
    }

    /**
     * Obtain attribute name for storing questions in the project attachment file
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getQuestionsFileAttributeName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _questionsFileAttributeName;
    }

    /**
     * Obtain attribute name for stop words
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getTranslationsCountAttributeName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _translatorsCountAttributeName;
    }


    /**
     * Obtain attribute name for stop words
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getTranslationsIterationCountAttributeName(WSContext context) throws Exception {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _translationsIterationCountAttributeName;
    }


    /**
     * Obtain attribute name that keeps the count of letters that user has translated
     *
     * @param context - WS context
     * @return - atribute name
     * @throws Exception - exception
     */
    public static String getTranslationRejectedAttributeName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _translationRejectedAttributeName;
    }

    /**
     * Obtain name and location of the global complexity rules XML file on AIS
     *
     * @param context - WS context
     * @return - AIS path to the XML global rules file for compexity handling
     * @throws Exception - exception
     */
    public static String getAssignmentRulesConfigurationFile(WSContext context) throws Exception {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _assignmentRuleConfigurationFileName;
    }

    /**
     * Obtain translator's workflow role name
     *
     * @param context - WS context
     * @return - name of the workflow role for Translator
     * @throws Exception - IO exception
     */
    public static String getTranslatorWorkflowRole(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _translatorWorkflowRole;
    }


    /**
     * Obtain supervisor's workflow role name
     *
     * @param context - WS context
     * @return - name of the workflow role for Supervisor
     * @throws Exception - IO exception
     */
    public static String getSupervisorWorkflowRole(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _supervisorWorkflowRole;
    }

    /**
     * Obtain translate step name
     *
     * @param context - WS context
     * @return - name of the Translate step
     * @throws Exception - IO exception
     */
    public static String getTranslateStepName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _translateStepName;
    }

    /**
     * Obtain update translation step name
     *
     * @param context - WS context
     * @return - name of the Update Translation step
     * @throws Exception - IO exception
     */
    public static String getUpdateTranslationStepName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _updateTranslationStepName;
    }

    /**
     * Obtain Perform QC step name
     *
     * @param context - WS context
     * @return - name of the "Perform QC" step
     * @throws Exception - IO exception
     */
    public static String getQCStepName(WSContext context) throws Exception {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _performQCStepName;
    }

    /**
     * Obtain translation queue step name
     *
     * @param context - WS context
     * @return - name of the Translation Queue step
     * @throws Exception - IO exception
     */
    public static String getTranslationQueueStepName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _translationQueueStepName;
    }

    /**
     * Obtain QC queue step name
     *
     * @param context - WS context
     * @return - name of the QC Queue step
     * @throws Exception - IO exception
     */
    public static String getQCQueueStepName(WSContext context) throws IOException {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _QCQueueStepName;
    }

    /**
     * Obtain Update Translation queue step name
     *
     * @param context - WS context
     * @return - name of the Update Translation Queue step
     * @throws Exception - IO exception
     */
    public static String getUpdateTranslationQueueStepName(WSContext context) throws Exception {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return _updateTranslationQueueStepName;
    }

    /**
     * Helper method to read the property value
     *
     * @param context      - WS context
     * @param key          - property key
     * @param defaultValue - default value
     * @return property value
     * @throws Exception - IO exception
     */
    public static String getString(WSContext context, String key, String defaultValue) throws Exception {
        if (!_initiated) {
            init(context);
        } else {
            refresh(context);
        }
        return safeGetString(key, defaultValue);
    }

    /**
     * Helper method to read the property value
     *
     * @param key          - property key
     * @param defaultValue - default value
     * @return property value
     */
    private static String safeGetString(String key, String defaultValue) {
        try {
            String val = _config.getProperty(key);
            if (val != null) {
                return val;
            }
        } catch (Exception e) {
            _log.warn("Could not load value for key: " + key);
        }
        if (defaultValue != null) {
            _log.debug("Using default value for key: " + key + " (" + defaultValue + ")");
        }
        return defaultValue;
    }

}
