package com.idiominc.ws.integration.compassion.autoaction.reporting;

import com.idiominc.ws.integration.compassion.utilities.AttributeValidationUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.workflow.WSTaskStep;
import org.apache.log4j.Logger;

/**
 * Collect escalation information for reporting purposes:
 * This step is added to the workflow following the Translate and "QC" step, only if Escalation was selected.
 * Project-level attribute Translation Escalated is configured in WorldServer.
 *
 * Note this data will capture a single escalation data for all steps including translation, Perform QC and Update Translation.
 *
 * @author SDL Professional Services
 */
public class CollectEscalationInfo extends WSCustomTaskAutomaticAction {

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(CollectEscalationInfo.class);

    //variable
    private static final String _translationEscalated = "TranslationEscalated";
    private static final String _TranslateStepName = "Translate";
    private static final String _UpdateTranslationStepName = "Update Translation";
    private static final String _QCStepName = "Perform QC";
    private static final String _EscalationTransition = "Escalate to Supervisor";

    /**
     * Aggregate the escalation data
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //validate attributes
        if(!AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _translationEscalated, log)) {
            return new WSActionResult(WSActionResult.ERROR, "Not all attributes are setup in system; please check log!");
        }

        //test last step completed
        WSTaskStep previousStep = task.getCurrentTaskStep().getPreviousTaskStep();
        String prevStepName = previousStep.getWorkflowStep().getName();
        String prevTransitionName = task.getCurrentTaskStep().getPreviousTransition().getName();

        if(prevStepName != null &&
                (prevStepName.equals(_TranslateStepName) ||
                        prevStepName.equals(_UpdateTranslationStepName) ||
                        prevStepName.equals(_QCStepName))){
            // perform record aggregation for escalation
            if(prevTransitionName != null && prevTransitionName.equals(_EscalationTransition)) {
                // previous step was Translate/QC and Escalation transition was specified
                task.getProject().setAttribute(_translationEscalated,"true");
            } else {
                log.warn("This automatic action should only be placed immediately after Translate/Perform QC/Update Translation" +
                        " step following Escalation transition.");
            }
        } else {
            log.warn("This automatic action should only be placed immediately after Translate/Perform QC/Update Translation" +
                    " step following Escalation transition.");
        }

        return new WSActionResult(DONE,
                "Escalation data aggregation is completed for reporting purposes.");

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
        return "Collect Escalation Info";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records escalation information for report data.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return "1.0";
    }

}
