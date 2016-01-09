package com.idiominc.ws.integration.compassion;

import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSObject;
import com.idiominc.wssdk.costmodel.WSCostModel;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.workflow.WSHumanTaskStep;
import com.idiominc.wssdk.workflow.WSTask;

/**
 * Class used by the transcription/scanned letter editor to validate the user who is opening the UI who must belong to the list of assignees.
 *
 * @author SDL Professional Services
 */
public class CompassionSecurity {

    public static enum ACL {
        ACCESS_TASK
    }

    public static boolean test(WSContext context, ACL acl, WSObject... wsObj) throws ACLException {

        try {
            check(context, acl, wsObj);
            return true;
        } catch (ACLException e) {
            return false;
        }

    }


    public static void check(WSContext context, ACL acl, WSObject... wsObj) throws ACLException {

        switch (acl) {

            case ACCESS_TASK:

                WSTask t = (WSTask) wsObj[0];
                for (WSUser u : ((WSHumanTaskStep) t.getCurrentTaskStep()).getAllUserAssignees()) {
                    if (u.getId() == context.getUser().getId()) {
                        return;
                    }
                }

                throw new ACLException("User is is not permitted to access task #" + t.getId());

            default:

        }


    }

    public static class ACLException extends Exception {
        public ACLException(String message) {
            super(message);
        }
    }
}
