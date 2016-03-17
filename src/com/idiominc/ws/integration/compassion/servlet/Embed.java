package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class to embed JS and supporting files in custom UI components.
 */
public class Embed {


    private static final Logger log = Logger.getLogger(Embed.class);

    /**
     * @param context
     * @param hook
     * @param name
     * @throws WSAisException
     * @throws IOException
     * @depreacted Deprecated by moving JS to public directory rather than embedding the contents directly in the output
     */
    public static void embedJS(WSContext context, WSHtmlContainer hook, String name) throws WSAisException, IOException {
        //This will be officially deprecated once automated deployment of supporting files are completed
        //log.warn("EmbedJS has been deprecated!");
        hook.add("<script>\n");
        embed(context, hook, name);
        hook.add("\n</script>\n");

    }

    public static void embed(WSContext context, WSHtmlContainer hook, String name) throws WSAisException, IOException {

        // Load content from public web-directory rather than AIS; public dir was synced from AIS via Deployment...update() command.
        //WSNode transportCrateJS = context.getAisManager().getNode("/Customization/" + name);
        //WSNode transportCrateJS = context.getAisManager().getNode("/Customization/" + name);

        InputStream is = null;
        // Add try/finally for I/O
        try {
            is = new FileInputStream(new File(Deployment.getInstance(context).getPublicWebDir(), name));//transportCrateJS.getInputStream();
            String htmlContent = FileUtils.getStreamAsString(is);
            //htmlContent = replaceVariables(context, htmlContent); // deprecated
            hook.add(htmlContent);
        } finally {
            FileUtils.close(is);
        }
    }

    /***
     @depreacted Deprecated by moving JS to public directory rather than embedding the contents directly in the output

     private static String replaceVariables(WSContext context, String htmlFile) {
     return htmlFile.replaceAll("%PROXY%", "ws_ext?servlet=include_proxy&token=" + context.getSessionToken() + "&fileName=");
     }
     */


}
