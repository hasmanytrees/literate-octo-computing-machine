package com.idiominc.ws.integration.compassion.autoaction.reporting;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.AttributeValidationUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.workflow.WSTaskStep;
import org.apache.log4j.Logger;

/**
 * Collect rework information for reporting purposes:
 * This step is added to the workflow following a rejection transition from the Perform QC step.
 * Project-level attribute Translation Submitted for Re-work is configured in WorldServer.
 *
 * @author SDL Professional Services
 */
public class CollectReworkInfo extends WSCustomTaskAutomaticAction {

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(CollectReworkInfo.class);

    //variable
    private static final String _translationSubmittedForRework = "TranslationSubmittedForRework";
    private static String _QCStepName = "Perform QC";
    private static String _ReworkTransition = "Require Update";

    /**
     * Aggregate the rework data
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //setup step name variables from custom.properties
        try {
            _QCStepName = Config.getString(wsContext, "step_name.perform_qc", "Perform QC");
            _ReworkTransition = Config.getString(wsContext, "transition.ReworkTransition", "Require Update");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        //validate attributes
        if(!AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _translationSubmittedForRework, log)) {
            return new WSActionResult(WSActionResult.ERROR, "Not all attributes are setup in system; please check log!");
        }

        //test last step completed
        WSTaskStep previousStep = task.getCurrentTaskStep().getPreviousTaskStep();
        String prevStepName = previousStep.getWorkflowStep().getName();
        String prevTransitionName = task.getCurrentTaskStep().getPreviousTransition().getName();

        if(prevStepName != null && prevStepName.equals(_QCStepName)){
            // perform record aggregation for escalation
            if(prevTransitionName != null && prevTransitionName.equals(_ReworkTransition)) {
                // previous step was Perform QC and Require Update transition was specified
                task.getProject().setAttribute(_translationSubmittedForRework,"true");
            } else {
                log.warn("This automatic action should only be placed immediately after Perform QC step following Require Update transition.");
            }
        } else {
            log.warn("This automatic action should only be placed immediately after Perform QC step following Require Update transition.");
        }

        return new WSActionResult(DONE,
                "Rework data aggregation is completed for reporting purposes.");

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
        return "Collect Rework Info";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records rework information for report data.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return "1.0";
    }

}
