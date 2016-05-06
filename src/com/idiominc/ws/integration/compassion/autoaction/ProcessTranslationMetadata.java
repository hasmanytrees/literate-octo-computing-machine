package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.metadata.*;

//commons classes
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomProjectAutomaticActionWithParameters;
import com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.attribute.WSAttributable;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;

//log4j
import org.apache.log4j.Logger;

//java
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;

// W3C DOM
import org.w3c.dom.Document;

/**
 * Store translation metadata to task' and project's attribute.
 * The metadata values are based on XML payload attributes passed to WorldServer from SFDC via ESB.
 *
 * @author SDL Professional Services
 */
public class ProcessTranslationMetadata extends WSCustomProjectAutomaticActionWithParameters {

    //parameters
    private final String _ATTR_EXPECTED_PROJECT_DURATION = "_ATTR_EXPECTED_PROJECT_DURATION";

    //parameters to this automatic action
    private String expectedProjectDurationStr;

    //log
    private Logger log = Logger.getLogger(ProcessTranslationMetadata.class);

    //version
    private String version = "1.0";

    //output transition
    private static final String DONE_TRANSITION = "Done";

    /**
     * Sets due date to the project and its' tasks
     * @param tasks - project's tasks
     */
    private void setDueDates(WSAssetTask[] tasks) {
        WSProject project = tasks[0].getProject();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, Integer.parseInt(expectedProjectDurationStr));
        Date due = now.getTime();
        project.setDueDate(due);
        for(WSAssetTask t: tasks) {
            t.setDueDate(due);
        }
    }

    /**
     * Assign translation metadatra to projects and tasks
     * @param wsContext - WorldServer Context
     * @param tasks - project's tasks
     * @throws MetadataException - exception on assignment
     */
    private void setTranslationMetadata(WSContext wsContext,
                                        WSAssetTask[] tasks)
                                        throws MetadataException {

        boolean hasAppliedProjectAttributes = false;

        for(WSAssetTask task : tasks) {

            WSNode assetNode = task.getSourceAisNode();

            Document asset = AttributeValueIdentifier.init(assetNode.getFile());

            CIMetadataConfig metadataConfig = new CIMetadataConfig();

            // Need to get the letter direction attribute first
            String letterDirection = AttributeValueIdentifier.getValue(asset,CIMetadataConfig.getDirectionXpath());

            HashMap<String, CIMetadataInfo> metadataMap = metadataConfig.getMetadataMap(letterDirection);

            for(Map.Entry<String,CIMetadataInfo> entry: metadataMap.entrySet()) {

                CIMetadataInfo attribute = entry.getValue();

                if(attribute.getAttributeObject() == ATTRIBUTE_OBJECT.PROJECT &&
                  hasAppliedProjectAttributes) {
                    //skip assignment to the project - we have already done so
                    continue;
                }

                WSAttributable target = (attribute.getAttributeObject() == ATTRIBUTE_OBJECT.PROJECT)
                                    ?
                                    task.getProject()
                                    :
                                    task;

                String value;

                if(attribute.isMultiValue()) {
                    value = AttributeValueIdentifier.getValues(asset, attribute.getXPath());
                } else {
                    value = AttributeValueIdentifier.getValue(asset, attribute.getXPath());
                }

                if(value == null || 0 == value.length()) {
                  if(attribute.isMandatory()) {
                      throw new MetadataException("Value is not found for attribute " + attribute.getAttributeName());
                  }
                }

                AttributeValidator.validateAttribute(wsContext,
                                                    attribute.getAttributeName(),
                                                    attribute.getAttributeObject(),
                                                    attribute.getAttributeType(),
                                                    target,
                                                    value);
                if(null == value) {
                    switch(attribute.getAttributeType()) {
                        case BOOLEAN:
                            value = "true";
                            break;
                        case INTEGER:
                            value = "0";
                            break;
                        case TEXT:
                            value = "";
                            break;
                    }
                }

                if(ATTRIBUTE_TYPE.BOOLEAN == attribute.getAttributeType()) {
                    WSAttributeUtils.setBooleanAttribute(
                            this,
                            target,
                            attribute.getAttributeName(),
                            Boolean.parseBoolean(value.toLowerCase()));
                } else {
                  target.setAttribute(attribute.getAttributeName(), value);
                }

            } //went over all attributes

            hasAppliedProjectAttributes = true; //project attribute assignment is done
        }
    }

    /**
     * Entry point for AA execution
     * @param wsContext - WorldServer Context
     * @param tasks - project's tasks
     * @return DONE or ERROR
     */
    public WSActionResult execute(WSContext wsContext, WSAssetTask[] tasks) {

        try {

            //apply due dates
            setDueDates(tasks);

            //set Translation Metadata
            setTranslationMetadata(wsContext, tasks);

            //reassign translate step

        } catch(MetadataException e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        }

        return new WSActionResult(getReturns()[0], "Completed applying processing project metadata successfully.");
    }

    /**
     * @return Transitions that AA supports
     */
    public String[] getReturns() {
        return new String[] {DONE_TRANSITION};
    }

    /**
     * @return AA Name
     */
    public String getName() {
        return "Process Translation Metadata Priority Action";
    }

    /**
     * @return AA description
     */
    public String getDescription() {
        return "Automatically process translation metadata on newly created translation project.";
    }

    /**
     * @return AA version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Extracts values of parameters to this AA
     * @param parameters - map of names / values of supplied parameters
     * @throws WSInvalidParameterException - exception
     */
    protected void preLoadParameters(Map parameters) throws WSInvalidParameterException {
        expectedProjectDurationStr = preLoadParameter(parameters, _ATTR_EXPECTED_PROJECT_DURATION, true);
    }

    /**
     *
      * @return List of parameters expected by this AA
     */
    public WSParameter[] getParameters() {
         return new WSParameter[]{
            WSParameterFactory.createIntegerParameter(_ATTR_EXPECTED_PROJECT_DURATION, "Project Duration (days)", 4)
        };
    }

}