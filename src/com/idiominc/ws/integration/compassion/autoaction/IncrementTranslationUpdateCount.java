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
 * Increments count of translation update iterations a given comm-kit has completed.
 * This is used to count and track how many times a given translation is sent to the translator by the QCer for update.
 *
 * @author SDL Professional Services
 */
public class IncrementTranslationUpdateCount extends WSCustomTaskAutomaticAction {
    //version
    private String version = "1.0";

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(IncrementTranslationCount.class);

    /**
     * Increment translation iteration count
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //obtain count attribute
        String attrTCount = null;
        try {
            attrTCount = Config.getTranslationsIterationCountAttributeName(wsContext);
            AttributeValidator.validateAttribute(wsContext,
                    attrTCount,
                    ATTRIBUTE_OBJECT.TASK,
                    ATTRIBUTE_TYPE.INTEGER,
                    task,
                    "1");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR,
                    "Attribute " + attrTCount + " is misconfigured. " + e.getLocalizedMessage());
        }


        int translationIteration;
        if(null == task.getAttribute(attrTCount)
                || task.getAttribute(attrTCount).length() == 0) {
            log.info("No value set. Set initial count of translation iteration to 1.");
            translationIteration= 1;
        }
        else {
            translationIteration = WSAttributeUtils.getIntegerAttribute(this,
                    task,
                    attrTCount);
        }

        if(translationIteration < 0) {
            return new WSActionResult(WSActionResult.ERROR,
                    "Invalid number of translation iteration for task " +
                            task.getSourcePath() +
                            ": " + translationIteration);
        }

        task.setAttribute(attrTCount,
                String.valueOf(++translationIteration));

        return new WSActionResult(DONE,
                "Task " + task.getSourcePath() +
                        " has completed "
                        + translationIteration+ " translation iterations.");

    }


    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {DONE};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Account for Translation Iteration";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records translation iteration count for given task.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
