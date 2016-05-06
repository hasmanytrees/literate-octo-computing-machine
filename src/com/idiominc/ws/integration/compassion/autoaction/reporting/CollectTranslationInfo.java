package com.idiominc.ws.integration.compassion.autoaction.reporting;

import com.idiominc.ws.integration.compassion.utilities.AttributeValidationUtils;
import com.idiominc.ws.integration.compassion.utilities.TaskUtil;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.workflow.WSTaskStep;
import org.apache.log4j.Logger;

import java.util.Date;

/**
 * Collect translation information for reporting purposes:
 * This step is added to the workflow following the Translation Queue and "Translate" step.
 * Project-level attribute Translate Step Completed On is configured in WorldServer.
 * Project-level attribute Translate Step Completed By is configured in WorldServer.
 * Project-level attribute Translate Step Accepted On is configured in WorldServer.
 * Project-level attribute Translate Step Accepted By is configured in WorldServer.
 *
 * @author SDL Professional Services
 */
public class CollectTranslationInfo extends WSCustomTaskAutomaticAction {

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(CollectTranslationInfo.class);

    //variable
    private static final String _translateStepAcceptedBy = "TranslateStepAcceptedBy";
    private static final String _translateStepAcceptedOn = "TranslateStepAcceptedOn";
    private static final String _translateStepCompletedBy = "TranslateStepCompletedBy";
    private static final String _TranslateStepCompletedOn = "TranslateStepCompletedOn";
    private static final String _Translation_Queue_Stepname = "Translation Queue";
    private static final String _Translate_Stepname = "Translate";

    /**
     * Aggregate the translation acceptance & completion data
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //validate attributes
        if(!AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _translateStepAcceptedBy, log) ||
        !AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _translateStepAcceptedOn, log) ||
        !AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _translateStepCompletedBy, log) ||
        !AttributeValidationUtils.validateAttributeExists(wsContext, task.getProject().getClass(), _TranslateStepCompletedOn, log)) {

            return new WSActionResult(WSActionResult.ERROR, "Not all attributes are setup in system; please check log!");
        }

        //various metadata
        WSUser translator = task.getTaskHistory().getLastHumanStepUser();
        if(null == translator) {
            return new WSActionResult(WSActionResult.ERROR, "Can not determine asset's Translator");
        }
        log.info("Translator is " + translator.getFullName());

        //test last step completed
        WSTaskStep previousStep = TaskUtil.getPreviousInWorkflowStep(task);
        String prevStepName = previousStep.getWorkflowStep().getName();
        Date completedDate = previousStep.getCompletionDate();

        if(prevStepName != null && prevStepName.equals(_Translation_Queue_Stepname)) {
            // perform record aggregation for translation queue step
            task.getProject().setAttribute(_translateStepAcceptedBy,
                    translator.getFirstName() + " " + translator.getLastName() + " [" + translator.getUserName() + "]");
            task.getProject().setAttribute(_translateStepAcceptedOn, completedDate.toString());
        } else if(prevStepName != null && prevStepName.equals(_Translate_Stepname)){
            // otherwise perform record aggregation for translation step
            task.getProject().setAttribute(_translateStepCompletedBy,
                    translator.getFirstName() + " " + translator.getLastName() + " [" + translator.getUserName() + "]");
            task.getProject().setAttribute(_TranslateStepCompletedOn, completedDate.toString());
        } else {
            log.error("This automatic action must be used after Translate or Translation Queue step!");
            return new WSActionResult(WSActionResult.ERROR,
                    "This automatic action must be used after Translate or Translation Queue step!");
        }

        return new WSActionResult(DONE,
                "Translation data aggregation is completed for reporting purposes.");

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
        return "Collect Translation Info";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records translation information for report data.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return "1.0";
    }

}
