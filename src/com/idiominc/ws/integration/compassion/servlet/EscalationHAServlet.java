package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.wssdk.servlet.BaseSetAttributesNT;

import java.util.ArrayList;
import java.util.List;

/**
 * Human action servlet to allow Supervisors to select rework reason and comment
 */
public class EscalationHAServlet extends BaseSetAttributesNT {

    private static List<String> attributes = new ArrayList();

    static {
        attributes.add("reworkReason");
        attributes.add("reworkComment");
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
        return "1.0";
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
