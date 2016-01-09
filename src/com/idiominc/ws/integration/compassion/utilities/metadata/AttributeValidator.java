package com.idiominc.ws.integration.compassion.utilities.metadata;

//sdk
import com.idiominc.wssdk.attribute.WSAttributeDescriptorType;
import com.idiominc.wssdk.attribute.WSAttributable;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSTask;

//java
import java.util.*;

/**
 * The static class to validate attribute assignments
 *
 * @author SDL Professional Services
 */

public class AttributeValidator {

    private static Map<ATTRIBUTE_TYPE, WSAttributeDescriptorType[]> _attributesMapping =
                       new HashMap<ATTRIBUTE_TYPE, WSAttributeDescriptorType[]>();

    static {
        _attributesMapping.put(ATTRIBUTE_TYPE.TEXT,
                                 new WSAttributeDescriptorType[]
                                 {
                                   WSAttributeDescriptorType.TEXT_TYPE,
                                   WSAttributeDescriptorType.TEXT_AREA_TYPE,
                                   WSAttributeDescriptorType.LARGE_TEXT_AREA_TYPE
                                 }
                               );

        _attributesMapping.put(ATTRIBUTE_TYPE.SELECTOR,
                                 new WSAttributeDescriptorType[]
                                 {
                                   WSAttributeDescriptorType.SELECT_TYPE,
                                   WSAttributeDescriptorType.MULTI_SELECT_TYPE
                                 }
                               );

        _attributesMapping.put(ATTRIBUTE_TYPE.INTEGER,
                                 new WSAttributeDescriptorType[]
                                 {
                                   WSAttributeDescriptorType.INTEGER_POSITIVE_TYPE
                                 }
                               );
        _attributesMapping.put(ATTRIBUTE_TYPE.BOOLEAN,
                                 new WSAttributeDescriptorType[]
                                 {
                                   WSAttributeDescriptorType.BOOLEAN_TYPE
                                 }
                               );
    }

    /**
     * Validates the attempted assignment to an attribute
     * @param wsContext - worldserver context
     * @param attrName - the intenal attribute name
     * @param object - object (task or project) we want to assign the attribute to
     * @param type - supported attribute type
     * @param target - real WorldServer target (WSAttributable) - project or task
     * @param value - attribute value
     * @throws MetadataException thrown if the attempted assignment is invalid
     */
    public static void validateAttribute(WSContext wsContext,
                                         final String attrName,
                                         final ATTRIBUTE_OBJECT object,
                                         final ATTRIBUTE_TYPE type,
                                         final WSAttributable target,
                                         final String value
                                   ) throws MetadataException
    {
        if(null == attrName) {
            throw new MetadataException("Attribute name has not been defined");
        }

        List<String> configuredAttributeNames = new ArrayList<String>();
        if(object == ATTRIBUTE_OBJECT.PROJECT) {
           configuredAttributeNames = Arrays.asList(wsContext.getAttributeManager().getAttributeNames(WSProject.class));
        } else if(object == ATTRIBUTE_OBJECT.TASK) {
           configuredAttributeNames = Arrays.asList(wsContext.getAttributeManager().getAttributeNames(WSTask.class));
        } else if(object == ATTRIBUTE_OBJECT.USER) {
           configuredAttributeNames = Arrays.asList(wsContext.getAttributeManager().getAttributeNames(WSUser.class));
        }
        else if(object == ATTRIBUTE_OBJECT.WORKGROUP) {
          configuredAttributeNames = Arrays.asList(wsContext.getAttributeManager().getAttributeNames(WSWorkgroup.class));
        }

        if(!(configuredAttributeNames.contains(attrName))) {
           throw new MetadataException ("Attribute '" + attrName + "' has not been configured for the requested object type "
                                        + object);
        }

        if(!(_attributesMapping.containsKey(type))) {
           throw new MetadataException ("Attribute '" + attrName + "' has been configured in the not supported type");
        }

        WSAttributeDescriptorType descriptor = target.getAttributeValue(attrName).getDescriptor().getDescriptorType();
        WSAttributeDescriptorType[] supportedDescriptors = _attributesMapping.get(type);
        if(!Arrays.asList(supportedDescriptors).contains(descriptor)) {
            throw new MetadataException("The attribute '" + attrName + "' has invalid configuration in WorldServer");
        }

        switch(type) {
            case TEXT:
                break;
            case SELECTOR:
                break;
            case INTEGER:
                if(!(null == value)) {
                  try {
                    Integer.parseInt(value);
                  } catch (NumberFormatException e) {
                    throw new MetadataException("The value for attribute '" + attrName + "' is not numeric - " + value);
                  }
                }
                break;
            case BOOLEAN:
                if(null != value
                   &&
                   !("TRUE".equals(value.toUpperCase())
                     ||
                     "FALSE".equals(value.toUpperCase())
                    )
                ) {
                    throw new MetadataException("The value for attribute '" + attrName + "' is not boolean - " + value);
                }
        }

    }

}
