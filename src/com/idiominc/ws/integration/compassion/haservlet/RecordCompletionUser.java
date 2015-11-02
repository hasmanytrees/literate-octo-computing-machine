package com.idiominc.ws.integration.compassion.haservlet;

import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSWorkflowUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.servlet.WSCustomHumanActionServletWithUI;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSSDKDialog;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.attribute.WSAttributeDescriptor;
import com.idiominc.wssdk.attribute.WSAttributeDescriptorType;
import com.idiominc.wssdk.workflow.WSTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by bslack on 9/21/15.
 */
public class RecordCompletionUser extends WSCustomHumanActionServletWithUI {

    public String getUIToken() {
        return "compassion";
    }

    public boolean handleComplete(WSContext context, WSSDKDialog dialog, WSAssetTask[] tasks, HttpServletRequest request, HttpServletResponse response) {
        for (WSAssetTask t : tasks) {
            String stepName = WSWorkflowUtils.getWorkflowStepName(t);
            WSAttributeDescriptor attrDesc =  context.getAttributeManager().getAttributeDescriptor(
                    WSTask.class,
                    "completion_" + stepName
            );
            if (attrDesc == null) {
                context.getAttributeManager().createAttributeDescriptor(
                        new Class[] {WSTask.class},
                        "completion_" + stepName.hashCode(),
                        "completion_" + stepName.hashCode(),
                        "Records completing user for step " + stepName,
                        WSAttributeDescriptorType.USER_TYPE,
                        new String[]{"hidden"}
                );
            }

            WSAttributeUtils.setUserAttribute(this,t,attrDesc.getName(),context.getUser());
        }
        return true;
    }

    public String[] getSupportedTransitionNames() {
        return new String[0];
    }

    public String getName() {
        return "Record Completing User";
    }

    public String getDescription() {
        return getName();
    }

    public String getVersion() {
        return "0.1";
    }
}
