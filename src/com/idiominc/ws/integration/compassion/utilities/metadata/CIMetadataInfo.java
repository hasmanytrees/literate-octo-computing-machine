package com.idiominc.ws.integration.compassion.utilities.metadata;

/**
 * Created by cslack on 3/28/2016.
 */
public class CIMetadataInfo {

    private String XPath;
    private String attributeName;
    private ATTRIBUTE_TYPE attributeType;
    private ATTRIBUTE_OBJECT attributeObject;
    private boolean mandatory;
    private boolean multiValue;

    /**
     * Constructor
     * @param xpath - xpath to the element in XML file
     * @param name - internal attribute name
     * @param type - supported attribute type
     * @param applyTo - object to which attribute should be applied to
     * @param required - if attribute is required
     */
    CIMetadataInfo (String xpath,
                    String name,
                    ATTRIBUTE_TYPE type,
                    ATTRIBUTE_OBJECT applyTo,
                    boolean required,
                    boolean multiValue) {

        this.XPath = xpath;
        this.attributeName = name;
        this.attributeType = type;
        this.attributeObject = applyTo;
        this.mandatory = required;
        this.multiValue = multiValue;
    }


    public String getXPath() {
        return XPath;
    }

    public void setxPath(String xPath) {
        this.XPath = xPath;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public ATTRIBUTE_TYPE getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(ATTRIBUTE_TYPE attributeType) {
        this.attributeType = attributeType;
    }

    public ATTRIBUTE_OBJECT getAttributeObject() {
        return attributeObject;
    }

    public void setAttributeObject(ATTRIBUTE_OBJECT attributeObject) {
        this.attributeObject = attributeObject;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMultiValue() {
        return multiValue;
    }

    public void setMultiValue(boolean multiValue) {
        this.multiValue = multiValue;
    }
}
