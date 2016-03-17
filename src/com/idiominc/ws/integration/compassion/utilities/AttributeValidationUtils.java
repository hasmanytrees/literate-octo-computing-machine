package com.idiominc.ws.integration.compassion.utilities;

import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.attribute.WSAttributeDescriptor;
import org.apache.log4j.Logger;

/**
 * Attribute validation utilities
 *
 * @author SDL Professional Services
 */
public class AttributeValidationUtils {

    /**
     * Utility method to validate if given attribute exists in WorldServer
     * @param wsContext WorldServer Context
     * @param aClass WorldServer class object for the given attribute
     * @param attrName Attribute name
     * @param log WorldServer log
     * @return true if attribute exists; false otherwise
     */
    public static boolean validateAttributeExists(WSContext wsContext, Class aClass, String attrName, Logger log) {
        boolean retVal = true;

        WSAttributeDescriptor attrDesc = wsContext.getAttributeManager().getAttributeDescriptor(aClass, attrName);
        if(attrDesc == null) {
            // attribute is not defined
            log.error("Not found attribute: Expected attribute:" + attrName + " for Object:" + aClass);
            retVal = false;
        }
        return retVal;
    }

}
