package com.idiominc.ws.integration.compassion.servlet;

import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlContainer;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bslack on 9/23/15.
 */
public class Embed {


    public static void embedJS(WSContext context, WSHtmlContainer hook, String name) throws WSAisException, IOException {

        hook.add("<script>\n");
        embed(context, hook, name);
        hook.add("\n</script>\n");
    }

    public static void embed(WSContext context, WSHtmlContainer hook, String name) throws WSAisException, IOException {
        WSNode transportCrateJS = context.getAisManager().getNode("/Customization/" + name);

        InputStream is = transportCrateJS.getInputStream();
        String htmlContent = FileUtils.getStreamAsString(is);
        htmlContent = replaceVariables(context,htmlContent);
        hook.add(htmlContent);
        FileUtils.close(is);
    }

   private static String replaceVariables(WSContext context, String htmlFile) {
       return htmlFile.replaceAll("%PROXY%","ws_ext?servlet=include_proxy&token="+context.getSessionToken()+"&fileName=");
   }


}
