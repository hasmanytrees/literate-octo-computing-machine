package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.wssdk.WSUtils;
import com.idiominc.wssdk.WSContext;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by bslack on 1/6/16.
 */
public class Deployment {

    private final String _INC_DIR = "compassion_inc";
    private final String _CHANGE_FILE = ".updated";

    private static final Logger log = Logger.getLogger(Deployment.class);

    private File publicWebDir;
    private File aisConfigDir;
    private File updateTracker;

    private long lastUpdate = -1;

    private static Deployment _instance = null;

    public static Deployment getInstance(WSContext context) throws IOException {
        if (_instance == null) {
            _instance = new Deployment(context);
        }

        return _instance;
    }

    public File getShadowWebDir() {
        return new File(aisConfigDir, _INC_DIR);
    }

    public File getPublicWebDir() {
        return publicWebDir;
    }

    private Deployment(WSContext context) throws IOException {
        publicWebDir = new File(WSUtils.getWebAppDirectory(), _INC_DIR);
        if (!publicWebDir.exists())
            publicWebDir.mkdir();
        aisConfigDir = new File(WSUtils.getConfigDirectory(context), _INC_DIR);

        updateTracker = new File(publicWebDir, _CHANGE_FILE);
        if (!updateTracker.exists())
            if (!updateTracker.createNewFile()) {
                log.warn("Configuration failure. Could not create change tracker. Process can proceed but performance is impacted.");
            }
    }



    public void update() throws IOException {

        if (lastUpdate == -1 || updateTracker.lastModified() != lastUpdate) {
            FileUtils.copyDirectory(aisConfigDir, publicWebDir);
            lastUpdate = touch(updateTracker);
        }
    }


    private static long touch(File f) throws IOException {
        long t = System.currentTimeMillis();
        if (!f.exists())
            f.createNewFile();

        f.setLastModified(t);
        return t;
    }


}
