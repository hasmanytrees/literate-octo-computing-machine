package com.idiominc.ws.integration.compassion.utilities.twostepproject;

//sdk
import com.idiominc.wssdk.linguistic.WSLanguage;
import com.idiominc.wssdk.ais.*;

//profservice
import com.idiominc.ws.integration.profserv.commons.FileUtils;

//core
import com.idiominc.ws.sdkcore.linguistic.WSLanguageImp;

//java
import java.util.Map;
import java.util.Set;

/**
 * Helper class for two-step project creation
 */
public class ProjectCreationUtilities {

    /**
     * Get the ISO name for the new target locale - that would become the folder name
     * for the new target folder
     * @param lang - WorldServer language
     * @return language code (like de-DE)
     */
    public static String getLanguageCode(WSLanguage lang) {
        WSLanguageImp sdkLanguage = (WSLanguageImp)lang;
        if(sdkLanguage.getLanguage().getSubLanguageCode() == null) {
            return sdkLanguage.getLanguage().getPrimaryLanguageCode();
        } else {
            return sdkLanguage.getLanguage().getPrimaryLanguageCode() + "-" +
                sdkLanguage.getLanguage().getSubLanguageCode();
        }
    }

    /**
     * Copy AIS property from one node to another
     * @param originalNode - source AIS node
     * @param newNode - target AIS node
     * @throws WSAisException - AIS exception
     */
    public static void copyAISProperties(WSMetaDataNode originalNode, WSMetaDataNode newNode) throws WSAisException {
        Map<String,Object> properties = originalNode.getProperties();
        Set<String> keys = properties.keySet();
        for(String key: keys){
            if(key.equalsIgnoreCase(WSSystemPropertyKey.LOCALE.toString())) {
                continue;
            }
            if(key.equalsIgnoreCase(WSSystemPropertyKey.FILTER_CONFIGURATION.toString())) {
                continue;
            }
            newNode.setProperty(key, properties.get(key));
        }
    }

    /**
     * Copy leaf node to the different localtion
     * @param originalNode - source node
     * @param newNode - target node
     * @throws TwoStepProjectException - exception
     */
    public static void copyData(WSNode originalNode, WSNode newNode) throws TwoStepProjectException {
      try {
        FileUtils.copyFile(originalNode.getFile(), newNode.getFile());
      } catch (Exception e) {
          throw new TwoStepProjectException(e.getLocalizedMessage());
      }
    }
    
}
