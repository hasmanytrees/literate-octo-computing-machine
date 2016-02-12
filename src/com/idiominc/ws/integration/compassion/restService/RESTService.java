package com.idiominc.ws.integration.compassion.restService;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Class to support REST API invocation.
 *
 * @author SDL Professional Services
 */
public class RESTService {

    private HttpClient httpClient;
    private static OAuthToken _token = null;

    private String serverURL;
    private String client;
    private String secret;
    private String apiKey;

    Logger log = Logger.getLogger(RESTService.class);

    // Get an instance of REST service responsible for making REST call to the ESB server
    public static RESTService getInstance(WSContext context) throws IllegalArgumentException {

        try {
            return new RESTService(
                    Config.getRESTApiBaseURL(context),
                    Config.getRESTApiClient(context),
                    Config.restRESTApiSecret(context),
                    Config.getRESTApiKey(context)
            );

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private HttpClient getClient() throws IOException {

        if (httpClient == null) {
            httpClient = new HttpClient();
        }

        return httpClient;

    }

    /**
     * REST service object constructor
     * @param serverURL REST server URL
     * @param client Client application name; 'worldserver' by default
     * @param secret Secret string used as part of ESB REST OAuth
     * @param apiKey API key used as part of ESB REST OAuth
     */
    public RESTService(String serverURL, String client, String secret, String apiKey) {
        this.serverURL = serverURL + (serverURL.endsWith("/") ? "" : "/");
        this.client = client;
        this.secret = secret;
        this.apiKey = apiKey;
    }

    /**
     * Build and send out status update message to ESB via REST
     * @param context WorldServer context
     * @param task current task
     * @param kvs Key/Value pair passed in from Set ESB automatic action
     * @throws RESTException REST exception when making REST call
     * @throws IOException IO exception when building status update message
     */
    public void setESBStatus(WSContext context, WSAssetTask task, KV... kvs) throws RESTException, IOException {
        executeCommand(
                context,
                getToken(context),
                new PostMethod(),
                Config.getRESTApiBaseCommand(context) + "/" +
                        Config.getRESTApiESBStatusCommand(context) + "/" + ESBHelper.getId(task) + "/status",
                ESBHelper.buildStatusXML(task, kvs)

        );

    }

    /**
     * Build and send out return kit message to ESB via REST
     * @param context WorldServer context
     * @param task current task
     * @throws RESTException REST exception when making REST call
     * @throws IOException IO exception when building return message
     */
    public void sendReturnKit(WSContext context, WSAssetTask task) throws RESTException, IOException {
        executeCommand(
                context,
                getToken(context),
                new PutMethod(),
                Config.getRESTApiBaseCommand(context) + "/" +
                        Config.getRESTApiESBStatusCommand(context) + "/" + ESBHelper.getId(task),
                ESBHelper.buildReturnKitXML(task)
        );

    }


    public InputStream getImage(WSContext context, String dynamicURL, int page, int dpi) throws RESTException, IOException {

        return executeCommand(
                context,
                getToken(context),
                new GetMethod(),
                dynamicURL,
                new KV("format", "jpg"),
                new KV("pg", page + 1), // page 0 is ALL pages
                new KV("dpi", dpi)
        );

    }

    protected InputStream executeCommand(WSContext context, HttpMethodBase httpVerb, String path, KV... parameters) throws RESTException, IOException {
        return executeCommand(context, null, httpVerb, path, null, parameters, null, null);
    }

    protected InputStream executeCommand(WSContext context, OAuthToken oAuth, HttpMethodBase httpVerb, String path) throws RESTException, IOException {
        return executeCommand(context, oAuth, httpVerb, path, null, null, null, null);
    }

    protected InputStream executeCommand(WSContext context, OAuthToken oAuth, HttpMethodBase httpVerb, String path, String payload) throws RESTException, IOException {
        return executeCommand(context, oAuth, httpVerb, path, null, null, null, payload);
    }

    protected InputStream executeCommand(WSContext context, OAuthToken oAuth, HttpMethodBase httpVerb, String path, KV... parameters) throws RESTException, IOException {
        return executeCommand(context, oAuth, httpVerb, path, parameters, null, null, null);
    }

    protected InputStream executeCommand(WSContext context, OAuthToken oAuth, HttpMethodBase httpVerb, String path, KV[] queryParameters, KV[] bodyParameters, File fileContents, String payload) throws RESTException, IOException {
        try {
            return internalExecuteCommand(oAuth, httpVerb, path, queryParameters, bodyParameters, fileContents, payload);
        } catch (RESTException e) {
            if (e.getHttpCode() == 401) {
                return internalExecuteCommand(getToken(context, true), httpVerb, path, queryParameters, bodyParameters, fileContents, payload);
            }

            throw e;
        }
    }

    protected InputStream internalExecuteCommand(OAuthToken oAuth, HttpMethodBase httpVerb, String path, KV[] queryParameters, KV[] bodyParameters, File fileContents, String xmlBody) throws RESTException, IOException {

        StringBuilder pathBuilder = new StringBuilder();
        if (!path.startsWith("http")) pathBuilder.append(serverURL);
        pathBuilder.append(path);
        if (oAuth != null) pathBuilder.append("?api_key=").append(apiKey);
        if (queryParameters != null) {
            for (KV queryParameter : queryParameters) {
                pathBuilder.append("&").append(queryParameter.key()).append("=").append(queryParameter.value());
            }
        }

        System.out.println("pathBuilder.toString()=" + pathBuilder.toString());
        log.debug("--pathBuilder.toString()=" + pathBuilder.toString());
        httpVerb.setPath(pathBuilder.toString());
        httpVerb.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");

        if (oAuth == null) {
            httpVerb.addRequestHeader("Authorization", "Basic " + getEncodedCredentials(client, secret));
        } else {
            httpVerb.addRequestHeader("Authorization", oAuth.getType() + " " + oAuth.getToken());
        }

        if (bodyParameters != null || fileContents != null || xmlBody != null) {

            if (!(httpVerb instanceof EntityEnclosingMethod))
                throw new IOException("Can only send complex data with POST/PUT!");

            EntityEnclosingMethod complexHttpVerb = (EntityEnclosingMethod) httpVerb;

            List<NameValuePair> bodyRequestList = new ArrayList<NameValuePair>();
            if (bodyParameters != null && httpVerb instanceof PostMethod) {
                for (KV bodyParameter : bodyParameters) {
                    bodyRequestList.add(new NameValuePair(bodyParameter.key(), bodyParameter.value()));
                }
            }

            if (fileContents != null) {

                List<Part> cmdParts = new ArrayList<Part>();
                for (NameValuePair nvp : bodyRequestList) {
                    cmdParts.add(new StringPart(nvp.getName(), nvp.getValue()));
                }

                FilePart filePart = new FilePart("payload", fileContents);
                filePart.setContentType("application/octet");
                filePart.setCharSet(null);
                filePart.setTransferEncoding(null);
                cmdParts.add(filePart);

                complexHttpVerb.setRequestEntity(new MultipartRequestEntity(
                        cmdParts.toArray(new Part[cmdParts.size()]),
                        complexHttpVerb.getParams()
                ));


            } else {
                if (httpVerb instanceof PostMethod)
                    ((PostMethod) httpVerb).setRequestBody(bodyRequestList.toArray(new NameValuePair[bodyRequestList.size()]));
            }

            if (xmlBody != null) {
                httpVerb.setRequestHeader("Content-Type", "application/xml; charset=UTF-8");
                complexHttpVerb.setRequestEntity(new StringRequestEntity(xmlBody, "application/xml", "UTF-8"));
            }


        }

        getClient().executeMethod(httpVerb);

        if (httpVerb.getStatusCode() >= 400) {
            log.error("--API call failed:" + httpVerb.getStatusCode() + "[" + httpVerb.getResponseBodyAsString() + "]");
            throw new RESTException("API call failed", httpVerb.getStatusCode(), httpVerb.getResponseBodyAsString(), httpVerb.getResponseContentLength());
        }

        return httpVerb.getResponseBodyAsStream();

    }

    protected OAuthToken login(WSContext context) throws RESTException, IOException {

        Map<String, String> loginResponse = asMap((JSONObject) JSONValue.parse(asString(
                executeCommand(
                        context,
                        new PostMethod(),
                        Config.getRESTApiOAuthToken(context),
                        new KV("grant_type", "client_credentials"),
                        new KV("scope", "read write")

                ))));


        return new OAuthToken(
                loginResponse.get("token_type"),
                loginResponse.get("access_token"),
                Integer.parseInt(loginResponse.get("expires_in"))
        );

    }

    protected OAuthToken getToken(WSContext context) throws RESTException, IOException {
        return getToken(context, false);
    }

    protected OAuthToken getToken(WSContext context, boolean force) throws RESTException, IOException {

        if (force || _token == null || _token.isExpired()) {
            _token = login(context);
        }

        return _token;
    }

    private String asString(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileUtils.copyStream(is, bos);
        FileUtils.close(bos);
        FileUtils.close(is);
        return new String(bos.toByteArray());
    }


    private Map<String, String> asMap(JSONObject response) {
        Map<String, String> ret = new HashMap<String, String>();
        Set<String> keys = response.keySet();
        for (String k : keys) {
            ret.put(k, response.get(k).toString());
        }

        return ret;
    }

    private static String getEncodedCredentials(String client, String secret) {
        return new BASE64Encoder().encode(new String(client + ":" + secret).getBytes());
    }

}
