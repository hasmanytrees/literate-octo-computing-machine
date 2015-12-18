package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.wssdk.servlet.BaseSetAttributesNT;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.workflow.WSProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Human action servlet to allow Supervisors to select rework reason and comment
 */
public class EscalationHAServlet extends BaseSetAttributesNT {

    private static List<String> attributes = new ArrayList();

    private final static String _B2S = "Beneficiary To Supporter";

//    static {
//        attributes.add("reworkReason");
//        attributes.add("reworkComment");
//    }

    protected void customInit(WSContext context, WSAssetTask[] tasks) {
        if(tasks[0] == null) {
            //Unexpected! Return!
            if(attributes.isEmpty()) {
                attributes.add("reworkReason");
                attributes.add("reworkComment");
            }
            return;
        }

        WSProject proj = tasks[0].getProject();
        String direction = proj.getAttribute("Direction");
        if(direction != null && direction.equals(_B2S)) {
            if(attributes.contains("reworkReasonS2B")) {
                attributes.remove("reworkReasonS2B");
                attributes.remove("reworkComment");
                attributes.add("reworkReason");
                attributes.add("reworkComment");
            } else if(attributes.isEmpty()){
                attributes.add("reworkReason");
                attributes.add("reworkComment");
            }
        } else {
            if(attributes.contains("reworkReason")) {
                attributes.remove("reworkReason");
                attributes.remove("reworkComment");
                attributes.add("reworkReasonS2B");
                attributes.add("reworkComment");
            } else if(attributes.isEmpty()){
                attributes.add("reworkReasonS2B");
                attributes.add("reworkComment");
            }
        }
    }

    @Override
    public List<String> getAttributeNames() {
        return attributes;
    }

    @Override
    public String getName() {
        return "escalation_servlet";
    }

    @Override
    public String getDescription() {
        return "Supervisor Escalation Human-Action Servlet";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @Override
    public String getCustomUIToken() {
        return "compassion.ui";
    }

    @Override
    public AttributableType getAttributableType() {
        return AttributableType.TASK;
    }

    public String[] getSupportedTransitionNames() {
        return new String[]{"Send Back", "Uncorrectable Error Return"};
    }
}
