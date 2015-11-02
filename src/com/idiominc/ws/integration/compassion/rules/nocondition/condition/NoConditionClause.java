package com.idiominc.ws.integration.compassion.rules.nocondition.condition;

import com.idiominc.ws.integration.compassion.rules.nocondition.Generic;
import com.idiominc.wssdk.component.rule.WSConditionClauseComponent;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptor;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.rule.WSRule;

import java.util.Set;
import java.util.Map;

/**
 */
public class NoConditionClause extends WSConditionClauseComponent {

    public Set filter(WSContext context, WSRule rule, Set set, Map parameters) {
        return set;
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

    public String getDisplayNameForIncludingCondition(WSContext context) {
        return getName();
    }

    public String getDisplayName() {
        return getName();
    }

    public String getDisplayNameForExcludingCondition(WSContext context) {
        return getName();
    }


    public String getName() {
        return "No Condition";
    }

    public String getDescription() {
        return "no condition";

    }

    public String getDescriptionForIncludingCondition(WSContext context) {
        return getDescription();
    }

    public String getDescriptionForExcludingCondition(WSContext context) {
        return getDescription();
    }

}
