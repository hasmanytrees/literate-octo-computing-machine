package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.reassignment.*;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.workflow.WSHumanTaskStep;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

//log4j
import org.apache.log4j.Logger;

/**
 * Reassign next human step to user who completed last step.
 *
 * @author SDL Professional Services
 */
public class ReassignNextStepToRecentExecutor extends WSCustomTaskAutomaticAction {

    //version
    private String version = "1.0";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    //log
    private Logger log = Logger.getLogger(ReassignNextStepToRecentExecutor.class);


    /**
     * Assign user who completed last human step to the immediate next human step
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {
        WSHumanTaskStep hts = ReassignStep.getNextHumanTaskStep(task);
        if(null == hts) {
            log.error("Can not locate the very next human step to step " + task.getCurrentTaskStep().getWorkflowStep().getName());
            return new WSActionResult(WSActionResult.ERROR, "Can not locate the very next human step");
        }
        WSUser executor = ReassignStep.getPrevousTaskStepExecutor(task);
        if(null == executor) {
            return new WSActionResult(WSActionResult.ERROR, "Can not locate the user who completed the last human step in a workflow");
        }
        ReassignStep.reassign(hts, new WSUser[] {executor});
        return new WSActionResult(getReturns()[0], "Reassigned step " +
                                                   hts.getWorkflowStep().getName() +
                                                   " to " +  executor.getFullName()
                                                   + " (" + executor.getUserName() + ")");
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
        return "Reassign Step to Claimant";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Reassign the next human step in a workflow to the executor of the last human step";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
