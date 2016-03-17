package com.idiominc.ws.integration.compassion.autoaction.reporting;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.utilities.AttributeValidationUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.workflow.WSTaskStep;
import org.apache.log4j.Logger;

import java.util.Date;

/**
 * Collect QC information for reporting purposes:
 * This step is added to the workflow following the QC Queue and "Perform QC" step.
 * Project-level attribute Perform QC Step Completed On is configured in WorldServer.
 * Project-level attribute Perform QC Step Completed By is configured in WorldServer.
 * Project-level attribute QC Queue Step Accepted On is configured in WorldServer.
 * Project-level attribute QC Queue Step Accepted By is configured in WorldServer.
 *
 *  @author SDL Professional Services
 */
public class CollectQCInfo extends WSCustomTaskAutomaticAction {

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(CollectQCInfo.class);

    //variable
    private static final String _qcStepAcceptedBy = "QCStepAcceptedBy";
    private static final String _qcStepAcceptedOn = "QCStepAcceptedOn";
    private static final String _qcStepCompletedBy = "QCStepCompletedBy";
    private static final String _qcStepCompletedOn = "QCStepCompletedOn";
    private static String _QC_Queue_Stepname = "QC Queue";
    private static String _QC_Stepname = "Perform QC";

    /**
     * Aggregate the QC acceptance & completion data
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //setup step name variables from custom.properties
        try {
            _QC_Queue_Stepname = Config.getString(wsContext, "step_name.qc_queue", "QC Queue");
            _QC_Stepname = Config.getString(wsContext, "step_name.perform_qc", "Perform QC");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        //validate attributes
        if(!AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _qcStepAcceptedBy, log) ||
            !AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _qcStepAcceptedOn, log) ||
            !AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _qcStepCompletedBy, log) ||
            !AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _qcStepCompletedOn, log)) {

            return new WSActionResult(WSActionResult.ERROR, "Not all attributes are setup in system; please check log!");
        }

        //various metadata
        WSUser translator = task.getTaskHistory().getLastHumanStepUser();
        if(null == translator) {
            return new WSActionResult(WSActionResult.ERROR, "Can not determine asset's QCer");
        }
        log.info("QCer is " + translator.getFullName());

        //test last step completed
        WSTaskStep previousStep = task.getCurrentTaskStep().getPreviousTaskStep();
        String prevStepName = previousStep.getWorkflowStep().getName();
        Date completedDate = previousStep.getCompletionDate();

        if(prevStepName != null && prevStepName.equals(_QC_Queue_Stepname)) {
            // perform record aggregation for QC queue step
            task.getProject().setAttribute(_qcStepAcceptedBy,
                    translator.getFirstName() + " " + translator.getLastName() + " [" + translator.getUserName() + "]");
            task.getProject().setAttribute(_qcStepAcceptedOn, completedDate.toString());
        } else if(prevStepName != null && prevStepName.equals(_QC_Stepname)){
            // otherwise perform record aggregation for QC step
            task.getProject().setAttribute(_qcStepCompletedBy,
                    translator.getFirstName() + " " + translator.getLastName() + " [" + translator.getUserName() + "]");
            task.getProject().setAttribute(_qcStepCompletedOn, completedDate.toString());
        } else {
            log.error("This automatic action must be used after Perform QC or QC Queue step!");
            return new WSActionResult(WSActionResult.ERROR,
                    "This automatic action must be used after Perform QC or QC Queue step!");
        }

        return new WSActionResult(DONE,
                "QC data aggregation is completed for reporting purposes.");

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
        return "Collect QC Info";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records QC information for report data.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return "1.0";
    }

}
