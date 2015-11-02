package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_OBJECT;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_TYPE;
import com.idiominc.ws.integration.compassion.utilities.metadata.AttributeValidator;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import org.apache.log4j.Logger;

/**
 * Check and confirm if stop-words are found or not.
 */
public class CheckForStopWords extends WSCustomTaskAutomaticAction {
    //version
    private String version = "1.0";

    //output transitions
    private static final String FOUND = "Stop-Words Found";
    private static final String NOT_FOUND = "Stop-Words Not Found";

    //log
    private Logger log = Logger.getLogger(CheckForStopWords.class);


    /**
     *  Determine if stop-words found
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //obtain and validate all attributes
        String attrStopWordsList; //stop words list. The name of this attribute is stopwordAttr

        try {

            attrStopWordsList = Config.getStopWordsAttributeName(wsContext);

            AttributeValidator.validateAttribute(wsContext,
                    attrStopWordsList,
                    ATTRIBUTE_OBJECT.TASK,
                    ATTRIBUTE_TYPE.TEXT,
                    task,
                    "");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                    "Attributes Misconfiguration. " + e.getLocalizedMessage());
        }

        try {
            String stopWords = WSAttributeUtils.getStringValue(wsContext,
                    task.getAttributeValue(attrStopWordsList));
            if(stopWords != null && stopWords.length() > 0) {
                return new WSActionResult(FOUND,
                        "Found at least one stop word.");
            }
        } catch(Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                    e.getLocalizedMessage());
        }


        //all tests results have been negative, so QC is not required
        return new WSActionResult(NOT_FOUND, "No Stop-Words are found.");
    }


    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {FOUND, NOT_FOUND};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Stop-Words Found?";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Determine if any stop-words have been identified in translation.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}