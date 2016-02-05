package com.idiominc.ws.integration.compassion.autoaction.reporting;

import com.idiominc.ws.integration.compassion.utilities.AttributeValidationUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.workflow.WSTaskStep;
import org.apache.log4j.Logger;

/**
 * Collect return to queue information for reporting purposes:
 * This step is added to the workflow following the Translate and "QC" step, only if Return to queue was selected.
 * Project-level attribute Returned to Queue is configured in WorldServer.
 *
 * @author SDL Professional Services
 */
public class CollectTranslationDelay extends WSCustomTaskAutomaticAction {

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(CollectTranslationDelay.class);

    //variable
    private static final String _returnedToQueue = "ReturnedToQueue";
    private static final String _TranslateStepName = "Translate";
    private static final String _QCStepName = "Perform QC";
    private static final String _returnToQueueTransition = "Return to Queue";

    /**
     * Aggregate the translation completion data
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //validate attributes
        if(!AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _returnedToQueue, log)) {
            return new WSActionResult(WSActionResult.ERROR, "Not all attributes are setup in system; please check log!");
        }

        //test last step completed
        WSTaskStep previousStep = task.getCurrentTaskStep().getPreviousTaskStep();
        String prevStepName = previousStep.getWorkflowStep().getName();
        String prevTransitionName = task.getCurrentTaskStep().getPreviousTransition().getName();

        if(prevStepName != null &&
                (prevStepName.equals(_TranslateStepName) || prevStepName.equals(_QCStepName))){
            // perform record aggregation for escalation
            if(prevTransitionName != null && prevTransitionName.equals(_returnToQueueTransition)) {
                // previous step was Translate/QC and return to queue transition was specified
                task.getProject().setAttribute(_returnedToQueue,"true");
            } else {
                log.warn("This automatic action should only be placed immediately after Translate/QC step " +
                        "following Return to Queue transition.");
            }
        } else {
            log.warn("This automatic action should only be placed immediately after Translate/QC step " +
                    "following Return to Queue transition.");
        }

        return new WSActionResult(DONE,
                "Return to queue data aggregation is completed for reporting purposes.");

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
        return "Collect Translation Delay Info";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records translation delay information for report data.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return "1.0";
    }

}
