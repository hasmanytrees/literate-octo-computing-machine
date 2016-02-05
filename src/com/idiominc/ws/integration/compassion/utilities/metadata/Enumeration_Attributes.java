package com.idiominc.ws.integration.compassion.utilities.metadata;

/**
 * The explicit list of attributes that need to be assigned to the task or project
 *
 * @author SDL Professional Services
 */

public enum Enumeration_Attributes {

    CompassionKitId ("SBCCommunicationDetails/CompassionSBCId", "CompassionSBCId", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    KitComplexity ("SBCCommunicationDetails/TranslationComplexity", "TranslationComplexity", ATTRIBUTE_TYPE.INTEGER, ATTRIBUTE_OBJECT.PROJECT, true),
    GlobalPartnerSetup ("SBCCommunicationDetails/GlobalPartner/OptInForLanguageTranslation", "GlobalPartner", ATTRIBUTE_TYPE.BOOLEAN, ATTRIBUTE_OBJECT.PROJECT, true),
    MandatoryReview ("SBCCommunicationDetails/Supporter/MandatoryReviewRequired", "MandatoryReview", ATTRIBUTE_TYPE.BOOLEAN, ATTRIBUTE_OBJECT.PROJECT, true),
    Direction ("SBCCommunicationDetails/Direction", "Direction", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    BeneficiaryName ("SBCCommunicationDetails/Beneficiary/Name", "BeneficiaryName", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    BeneficiaryGender ("SBCCommunicationDetails/Beneficiary/Gender", "BeneficiaryGender", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    BeneficiaryAge ("SBCCommunicationDetails/Beneficiary/Age", "BeneficiaryAge", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    OriginalLanguage ("SBCCommunicationDetails/OriginalLanguage", "OriginalLanguage", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    TranslationLanguage ("SBCCommunicationDetails/TranslationLanguage", "TranslationLanguage", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    FieldOfficeName ("SBCCommunicationDetails/FieldOffice/Name", "FOName", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    GlobalPartnerId ("SBCCommunicationDetails/GlobalPartner/Id", "GlobalPartnerId", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    SDLProcessRequired ("SBCCommunicationDetails/SDLProcessRequired", "SDLProcessRequired", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true),
    TemplateID ("SBCCommunicationDetails/Template", "TemplateID", ATTRIBUTE_TYPE.TEXT, ATTRIBUTE_OBJECT.PROJECT, true);

    //xpath to attribute tag in the source asset
    private final String xPath;

    //attribute name
    private final String attributeName;

    //attribute type
    private final ATTRIBUTE_TYPE attrType;

    //an object attribute should be applied to
    private final ATTRIBUTE_OBJECT attrObject;

    //is attribute required?
    private final boolean isAttrRequired;


    /**
     * Constructor
     * @param xpath - xpath to the element in XML file
     * @param name - internal attribute name
     * @param type - supported attribute type
     * @param applyTo - object to which attribute should be applied to
     * @param required - if attribute is required
     */
    Enumeration_Attributes (String xpath,
                            String name,
                            ATTRIBUTE_TYPE type,
                            ATTRIBUTE_OBJECT applyTo,
                            boolean required) {
           this.xPath = xpath;
           this.attributeName = name;
           this.attrType = type;
           this.attrObject = applyTo;
           this.isAttrRequired = required;
    }

    /**
     * Get XPath to the attribute
     * @return xpath
     */
    public String getXPath() {
           return this.xPath;
    }

    /**
     * Obtain attribute internal name in WS
     * @return attribute name
     */
    public String getAttributeName() {
        return this.attributeName;
    }

    /**
     * Obtain supported attribute type
     * @return Attribute type (supported)
     */
    public ATTRIBUTE_TYPE getAttributeType() {
        return this.attrType;
    }

    /**
     * Obtain supported object to which attribute will be applied to
     * @return Attributble object
     */
    public ATTRIBUTE_OBJECT getAttributeObject() {
        return this.attrObject;
    }

    /**
     * Check if attribute is mandatory
     * @return true if attrinute is mandatory, false otherwise
     */
    public boolean isMandatory() {
        return this.isAttrRequired;
    }

}
