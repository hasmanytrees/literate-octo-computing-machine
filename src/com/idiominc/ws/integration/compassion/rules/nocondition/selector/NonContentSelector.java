package com.idiominc.ws.integration.compassion.rules.nocondition.selector;


import com.idiominc.ws.integration.compassion.rules.nocondition.Generic;
import com.idiominc.wssdk.component.rule.WSSelectorClauseComponent;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptor;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.rule.WSRule;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;

/**
 */
public class NonContentSelector extends WSSelectorClauseComponent {

    public Set select(WSContext context, WSRule rule, Map parameters) {
        Set ret = new HashSet();
        ret.add(new Generic());
        return ret;
    }

    public Class getSupportedClass() {
        return Generic.class;
    }


    public WSClauseParameterDescriptor[] getParameters() {
        return new WSClauseParameterDescriptor[0];
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDisplayName(WSContext context) {
        return getName();
    }

    public String getName() {
        return "Non-Content Based";
    }

    public String getDescription() {
        return "no content";

    }

    public String getDescription(WSContext context) {
        return getDescription();
    }

}