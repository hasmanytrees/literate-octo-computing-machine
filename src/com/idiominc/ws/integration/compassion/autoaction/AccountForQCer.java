package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.user.WSUser;
import org.apache.log4j.Logger;

/**
 * Identify and store the QCer who worked on given translation task as project attribute
 *
 * @author SDL Professional Services
 */
public class AccountForQCer extends WSCustomTaskAutomaticAction {

    //version
    private String version = "1.0";

    //output transitions
    private static final String DONE = "Done";

    //log
    private Logger log = Logger.getLogger(AccountForQCer.class);

    //variable
    private static final String _mostRecentQCAttr = "MostRecentQCer";

    /**
     * Save the QCer who completed the work
     * @param wsContext - WorldServer Context
     * @param task - project's task
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask task) {

        //various metadata
        WSUser translator = task.getTaskHistory().getLastHumanStepUser();

        if(null == translator) {
            return new WSActionResult(WSActionResult.ERROR, "Can not determine asset's QCer");
        }
        log.info("QCer is " + translator.getFullName());


        String newQCer = translator.getFirstName() + " " + translator.getLastName() + " [" + translator.getUserName() + "]";
        String lastQCer = task.getProject().getAttribute(_mostRecentQCAttr);
        if(lastQCer != null && !lastQCer.equals("") && !lastQCer.equals(newQCer)) {
            lastQCer = lastQCer + "|" + newQCer;
        } else {
            lastQCer = newQCer;
        }
        task.getProject().setAttribute(_mostRecentQCAttr, lastQCer);


        return new WSActionResult(DONE,
                "User " + translator.getFullName() + " ("
                        + translator.getUserName() + ") has completed QC step.");

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
        return "Account for QC";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Records information that user has QCed this asset";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

}
