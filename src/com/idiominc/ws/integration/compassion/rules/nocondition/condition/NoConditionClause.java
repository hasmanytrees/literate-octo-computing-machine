package com.idiominc.ws.integration.compassion.rules.nocondition.condition;

import com.idiominc.ws.integration.compassion.rules.nocondition.Generic;
import com.idiominc.wssdk.component.rule.WSConditionClauseComponent;
import com.idiominc.wssdk.component.rule.WSClauseParameterDescriptor;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.rule.WSRule;

import java.util.Set;
import java.util.Map;

/**
 * Default no-condition clause for use with custom business rule in WorldServer where a rule will be setup with no specific condition.
 * With no-condition, configured business rule will execute upon schedule without confirming the condition.
 *
 * @author SDL Professional Services
 */
public class NoConditionClause extends WSConditionClauseComponent {

    // No specific filter is set/required for this clause
    public Set filter(WSContext context, WSRule rule, Set set, Map parameters) {
        return set;
    }

    // Include support for Generic object for this no-condition business rule clause
    public Class getSupportedClass() {
        return Generic.class;
    }

    // No parameters
    public WSClauseParameterDescriptor[] getParameters() {
        return new WSClauseParameterDescriptor[0];
    }

    public String getVersion() {
        return "1.0";
    }

    // Return default no-condition clause name
    public String getDisplayNameForIncludingCondition(WSContext context) {
        return getName();
    }

    // Return default no-condition clause name
    public String getDisplayName() {
        return getName();
    }

    // Return default no-condition clause name
    public String getDisplayNameForExcludingCondition(WSContext context) {
        return getName();
    }

    // Default no-condition clause name
    public String getName() {
        return "No Condition";
    }

    // No-condition clause description for use in business rule definition
    public String getDescription() {
        return "no condition";

    }

    // Return default no-condition clause description
    public String getDescriptionForIncludingCondition(WSContext context) {
        return getDescription();
    }

    // Return default no-condition clause description
    public String getDescriptionForExcludingCondition(WSContext context) {
        return getDescription();
    }

}
