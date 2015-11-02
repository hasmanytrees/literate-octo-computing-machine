package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.ws.integration.compassion.TranscriptionDataService;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticAction;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

/**
 * Created by bslack on 9/23/15.
 */
public class MergeTranscribedContent extends WSCustomTaskAutomaticAction {

    private static final String _TRANSITION_DONE = "Done";

    public WSActionResult execute(WSContext context, WSAssetTask task) {
        TranscriptionDataService.mergeTranscription(task, true);
        return new WSActionResult(_TRANSITION_DONE, "Updated source content with transcribed text");
    }

    public String[] getReturns() {
        return new String[]{_TRANSITION_DONE};
    }

    public String getName() {
        return "Merge Transcription";
    }

    public String getDescription() {
        return "Merges any transcribed working content with the source asset";
    }
}
