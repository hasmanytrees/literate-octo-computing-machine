package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.ws.integration.compassion.utilities.MergeUtil;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticActionWithParameters;
import com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;
import com.idiominc.wssdk.component.autoaction.WSActionResult;

import java.util.Map;

/**
 * A generic automatic action for copying one XML text field to another.  The fields need to be specified in the
 * AA parameters as well as which AIS mount we should be operating on, source or target.
 *
 * Created by cslack on 3/28/2016.
 */
public class copyXMLTextContent extends WSCustomTaskAutomaticActionWithParameters {

    private static final String _TRANSITION_DONE = "Done";

    private static final String _ATTR_ORIGINAL_TEXT = "originalText";
    private static final String _ATTR_TARGET_TEXT = "targetText";
    private static final String _ATTR_AIS_MOUNT = "aisMount";
    private static final String _ATTR_AIS_MOUNT_VALUES[] = {"Source", "Target"};

    private String originalXMLField = null;
    private String targetXMLField = null;
    private String aisMountType = null;

    public WSActionResult execute(WSContext context, WSAssetTask task) {

        WSNode aisNode = null;

        switch(aisMountType) {
            case "Source":
                aisNode = task.getSourceAisNode();
                break;
            case "Target":
                aisNode = task.getTargetAisNode();
                break;
        }

        MergeUtil.copyPayloadTextFields(
                task,
                aisNode,    // The AIS position to be working on
                originalXMLField,           // The original text field value
                targetXMLField              // The text field to copy to
        );

        return new WSActionResult(_TRANSITION_DONE, "Updated Translated text with English translated content");
    }


    // Get automatic action parameters
    public WSParameter[] getParameters() {
        return new WSParameter[]{
                WSParameterFactory.createStringParameter(_ATTR_ORIGINAL_TEXT, "Original Text Field", ""),
                WSParameterFactory.createStringParameter(_ATTR_TARGET_TEXT, "Target Text Field", ""),
                WSParameterFactory.createSelectorParameter(_ATTR_AIS_MOUNT, "AIS Mount Type", _ATTR_AIS_MOUNT_VALUES),
        };
    }

    // Pre-load the automatic action parameter values
    protected void preLoadParameters(Map parameters) throws WSInvalidParameterException {
        originalXMLField = preLoadParameter(parameters, _ATTR_ORIGINAL_TEXT, false);
        targetXMLField = preLoadParameter(parameters, _ATTR_TARGET_TEXT, false);
        aisMountType = preLoadParameter(parameters, _ATTR_AIS_MOUNT, false);
    }

    public String[] getReturns() {
        return new String[]{_TRANSITION_DONE};
    }
    public String getName() {
        return "Copy XML Text Content";
    }

    public String getDescription() {
        return "Copies XML text field to another text field";
    }
}
