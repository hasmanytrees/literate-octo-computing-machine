package com.idiominc.ws.integration.compassion.utilities;

import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.workflow.*;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by cslack on 3/28/2016.
 */
public class TaskUtil {

    private static Logger log = Logger.getLogger(TaskUtil.class);

    /**
     * When getting an immediately previous step we need to account for the possibility that the step may have gone
     * into auto error and deal with it accordingly.
     *
     * @param task - WS Task
     * @return
     */
    public static WSTaskStep getPreviousInWorkflowStep(WSAssetTask task) {

        // Get the previous step
        WSTaskStep previousStep = task.getCurrentTaskStep().getPreviousTaskStep();

        // Make sure this exists and that it's an auto error
        if (previousStep != null && previousStep.getWorkflowStep().getName().equals("Auto Error")) {

            // If the previous step was auto error then we really need to get the step it would have been if it
            // was NOT auto error, so get all the steps
            WSTaskStep[] steps = task.getSteps();

            // Sort the steps by completion date
            Arrays.sort(steps, new Comparator<WSTaskStep>() {
                @Override
                public int compare(WSTaskStep o1, WSTaskStep o2) {
                    Date o1Date= o1.getCompletionDate();
                    Date o2Date = o2.getCompletionDate();

                    if(o1Date == null) {
                        o1Date = new Date(0);
                    }

                    if(o2Date == null) {
                        o2Date = new Date(0);
                    }

                    return o1Date.compareTo(o2Date);
                }
            });

            // Get the first step back in the stack that isn't an auto error
            for(int i = (steps.length - 1); i >= 0; i--) {
                if(!steps[i].getWorkflowStep().getName().equals("Auto Error")) {
                    // This should be the last step run before the current step
                    previousStep = steps[i];
                    break;
                }
            }
        }

        return previousStep;

    }
}