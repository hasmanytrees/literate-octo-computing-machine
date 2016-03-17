package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.reassignment.*;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.workflow.WSHumanTaskStep;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

//log4j
import org.apache.log4j.Logger;

/**
 * Reassign the QC queue step to the list of eligible translators.
 * QC eligibility follows similar requirements as the translation step assignment based on the complexity of the communication kit,
 * translator rating of given user, and who last completed the translation.
 *
 * [Enhancement March 2016] To support dynamic assignment, reassign logic has been factored out for easy sharing between
 * this automatic action and custom business rule.
 *
 * @author SDL Professional Services
 */
public class ReassignQCStep  extends WSCustomTaskAutomaticAction {

    //version
    private String version = "2.0";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    //log
    private Logger log = Logger.getLogger(ReassignQCStep.class);

    /**
     * Reassign QC Queue Step
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        StringBuilder returnMsg = new StringBuilder();
        WSHumanTaskStep hts = ReassignStep.getNextHumanTaskStep(task);
        boolean success = ReassignStep.reassignQCQueue(wsContext, task, log, returnMsg, this, hts);

        if(success) {
            return new WSActionResult(DONE_TRANSITION, returnMsg.toString());
        } else {
            return new WSActionResult(WSActionResult.ERROR, returnMsg.toString());
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
        return "Reassign QC Queue Step";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Reassign QC Queue Step to eligible translators based on " +
                " workflow role, workgroup, language, and translation complexity rating";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }
    
}
