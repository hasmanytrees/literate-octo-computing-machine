package com.idiominc.ws.integration.compassion.utilities.qc;

import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSUtils;
import org.apache.log4j.Logger;

//java
import java.util.*;
import java.io.File;
import java.io.FileInputStream;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;



/**
 * Externaliztion of QC Sampling Rate into the simple properties file
 * <Rating>=<sample rate>
 *
 * @author SDL Professional Services
 */
public class QCSamplingRate {

    //configuration file handling
    private static long _lastModified = 0;

    //constants
    private static Properties _config = new Properties();

    //logger
    private static Logger _log = Logger.getLogger(QCSamplingRate.class);

    //flag
    private static boolean _initiated = false;


    /**
     * Reinitialize properties file if it got changed
     * @param context - WS context
     * @throws Exception - exception (IO)
     */
    private static void refresh(WSContext context) throws Exception {
        try {
            File f = getConfigFile(context);
            if (f.lastModified() != _lastModified) {
                init(context);
            }
        } catch (Exception e) {
            _log.error(e.getLocalizedMessage());
            throw e;
        }
    }

    /**
     * Get the handle to this properties file
     * @param context - WS context
     * @return - File handle to the file
     * @throws Exception - IO exception
     */
    private static File getConfigFile(WSContext context) throws Exception {
        File f = new File(WSUtils.getConfigNode(context).getFile(), "qcSampleRate.properties");
        if (!f.exists()) {
          throw new WSAisException("Can't locate QC sample configuration file " +
                                      f.getAbsolutePath() + "qcSampleRate.properties");
        }
        return f;
    }

    /**
     * Initialize this properties file and load it
     * @param context - WS context
     * @throws Exception - IO exception
     */
    private static void init(WSContext context) throws Exception {
        _initiated = true;
        try {
            File f = getConfigFile(context);
            _lastModified = f.lastModified();
            FileInputStream fis = new FileInputStream(f);
            _config.load(fis);
            FileUtils.close(fis);
        } catch (Exception e) {
            _log.error(e.getLocalizedMessage());
        }
    }

    /**
     * Obtain sample rating as it corresponds to the translator's rating
     * @param context - WS context
     * @param tRating - translator's rating
     * @return - AIS path to the XML global rules file for compexity handling
     * @throws Exception - exception
     */
    public static String getSampleRate(WSContext context,
                                       String tRating) throws Exception {
        if (!_initiated) {
            init(context);
        }  else {
            refresh(context);
        }
        return safeGetString(tRating, null);
    }


    /**
     * Helper method to read the property value
      * @param key - property key
     * @param defaultValue - default value
     * @return property value
     */
    private static String safeGetString(String key, String defaultValue) {
        try {
            String val = _config.getProperty(key);
            if (val != null) {
                return val;
            }
        } catch (Exception e) {
            _log.warn("Could not load value for key: " + key);
        }
        _log.debug("Using default value for key: " + key + " (" + defaultValue + ")");
        return defaultValue;
    }

}
