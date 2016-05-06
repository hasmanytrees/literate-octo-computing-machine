package com.idiominc.ws.integration.compassion.utilities.metadata;

import java.util.EnumMap;
import java.util.HashMap;

/**
 * Created by cslack on 3/28/2016.
 */
public  class CIMetadataConfig {


    private static EnumMap<ATTRIBUTE_LETTER_DIRECTION,HashMap<String,CIMetadataInfo>> metadataMap = new EnumMap<ATTRIBUTE_LETTER_DIRECTION,HashMap<String,CIMetadataInfo>>(ATTRIBUTE_LETTER_DIRECTION.class);
    private static String _LETTER_DIRECTION_XPATH = "SBCCommunicationDetails/Direction";

    private static HashMap<String,ATTRIBUTE_LETTER_DIRECTION> _DIRECTION_MAP = new HashMap<>();

    static {

        _DIRECTION_MAP.put("Supporter To Beneficiary", ATTRIBUTE_LETTER_DIRECTION.S2B);
        _DIRECTION_MAP.put("Beneficiary To Supporter", ATTRIBUTE_LETTER_DIRECTION.B2S);
        _DIRECTION_MAP.put("Third Party Letter", ATTRIBUTE_LETTER_DIRECTION.THIRD_PARTY);

        standardLetterTypes();
        thirdPartyLetter();
    }

    private static void standardLetterTypes() {

        HashMap<String, CIMetadataInfo> metadataConfig = new HashMap<>();

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/CompassionSBCId",
                "CompassionSBCId",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/TranslationComplexity",
                "TranslationComplexity",
                ATTRIBUTE_TYPE.INTEGER,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/GlobalPartner/OptInForLanguageTranslation",
                "GlobalPartner",
                ATTRIBUTE_TYPE.BOOLEAN,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/Supporter/MandatoryReviewRequired",
                "MandatoryReview",
                ATTRIBUTE_TYPE.BOOLEAN,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                _LETTER_DIRECTION_XPATH,
                "Direction",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/Beneficiary/Name",
                "BeneficiaryName",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/Beneficiary/Gender",
                "BeneficiaryGender",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/Beneficiary/Age",
                "BeneficiaryAge",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/OriginalLanguage",
                "OriginalLanguage",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/TranslationLanguage",
                "TranslationLanguage",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/FieldOffice/Name",
                "FOName",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/GlobalPartner/Id",
                "GlobalPartnerId",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/SDLProcessRequired",
                "SDLProcessRequired",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        metadataMap.put(ATTRIBUTE_LETTER_DIRECTION.B2S,metadataConfig);
        metadataMap.put(ATTRIBUTE_LETTER_DIRECTION.S2B,metadataConfig);

    }



    private static void thirdPartyLetter() {

        HashMap<String, CIMetadataInfo> metadataConfig = new HashMap<>();

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/CompassionSBCId",
                "CompassionSBCId",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/TranslationComplexity",
                "TranslationComplexity",
                ATTRIBUTE_TYPE.INTEGER,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/GlobalPartner/OptInForLanguageTranslation",
                "GlobalPartner",
                ATTRIBUTE_TYPE.BOOLEAN,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                _LETTER_DIRECTION_XPATH,
                "Direction",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/OriginalLanguage",
                "OriginalLanguage",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/TranslationLanguage",
                "TranslationLanguage",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/FieldOffice/Name",
                "FOName",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/GlobalPartner/Id",
                "GlobalPartnerId",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/SDLProcessRequired",
                "SDLProcessRequired",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                true,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/Cluster/Name",
                "ClusterName",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                false,
                false);

        addMetadataToMap(metadataConfig,
                "SBCCommunicationDetails/Pages/OriginalQuestion",
                "OriginalQuestion",
                ATTRIBUTE_TYPE.TEXT,
                ATTRIBUTE_OBJECT.PROJECT,
                false,
                true);

        metadataMap.put(ATTRIBUTE_LETTER_DIRECTION.THIRD_PARTY,metadataConfig);
    }


    private static void addMetadataToMap(
            HashMap<String, CIMetadataInfo> metadataConfig,
            String xPath,
            String name,
            ATTRIBUTE_TYPE attrType,
            ATTRIBUTE_OBJECT attrObj,
            boolean required,
            boolean multiValue) {

        metadataConfig.put(name,new CIMetadataInfo(xPath, name, attrType, attrObj, required, multiValue));
    }


    public static HashMap<String, CIMetadataInfo> getMetadataMap(ATTRIBUTE_LETTER_DIRECTION direction) {
        return metadataMap.get(direction);
    }

    public static HashMap<String, CIMetadataInfo> getMetadataMap(String direction) {
        return metadataMap.get(_DIRECTION_MAP.get(direction));
    }

    public static CIMetadataInfo getMetadataItem(ATTRIBUTE_LETTER_DIRECTION direction, String attrName) {
        return metadataMap.get(direction).get(attrName);
    }

    public static CIMetadataInfo getMetadataItem(String direction, String attrName) {
        return metadataMap.get(_DIRECTION_MAP.get(direction)).get(attrName);
    }

    public static String getDirectionXpath() {
        return _LETTER_DIRECTION_XPATH;
    }

}
