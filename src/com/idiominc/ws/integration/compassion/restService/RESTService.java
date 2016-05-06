package com.idiominc.ws.integration.compassion.restService;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSAssetTask;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Level;
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
import java.util.concurrent.TimeUnit;


/**
 * Class to support REST API invocation.
 *
 * @author SDL Professional Services
 */
public class RESTService {

    private static final int _MAX_RETRY = 4;

    private HttpClient httpClient;
    private static OAuthToken _token = null;

    private String serverURL;
    private String client;
    private String secret;
    private String apiKey;

    Logger log = Logger.getLogger(RESTService.class);


    // Get an instance of REST service responsible for making REST call to the ESB server
    public static RESTService getInstance(WSContext context) throws IllegalArgumentException {

        Logger.getLogger(HttpMethodBase.class).setLevel(Level.ERROR);

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
            httpClient.getParams().setParameter("http.protocol.single-cookie-header", true);
            httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        }

        return httpClient;

    }

    private void foo() {

    }

    /**
     * REST service object constructor
     *
     * @param serverURL REST server URL
     * @param client    Client application name; 'worldserver' by default
     * @param secret    Secret string used as part of ESB REST OAuth
     * @param apiKey    API key used as part of ESB REST OAuth
     */
    public RESTService(String serverURL, String client, String secret, String apiKey) {
        this.serverURL = serverURL + (serverURL.endsWith("/") ? "" : "/");
        this.client = client;
        this.secret = secret;
        this.apiKey = apiKey;
    }

    /**
     * Build and send out status update message to ESB via REST
     *
     * @param context WorldServer context
     * @param task    current task
     * @param kvs     Key/Value pair passed in from Set ESB automatic action
     * @throws RESTException REST exception when making REST call
     * @throws IOException   IO exception when building status update message
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
     *
     * @param context WorldServer context
     * @param task    current task
     * @throws RESTException REST exception when making REST call
     * @throws IOException   IO exception when building return message
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


    public InputStream getImage(WSContext context, String dynamicURL, int page, int width, int quality) throws RESTException, IOException {
//    public InputStream getImage(WSContext context, String dynamicURL, int page, int dpi) throws RESTException, IOException {

        return executeCommand(
                context,
                getToken(context),
                new GetMethod(),
                dynamicURL,
                new KV("format", "jpg"),
                (page == -1 ? null : new KV("pg", page + 1)), // page 0 is ALL pages
//                new KV("dpi", dpi)
                new KV("width", width),
                new KV("quality", quality)
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
            return internalExecuteCommand(oAuth, httpVerb, path, queryParameters, bodyParameters, fileContents, payload, 0);
        } catch (RESTException e) {
            if (e.getHttpCode() == 401) {
                return internalExecuteCommand(getToken(context, true), httpVerb, path, queryParameters, bodyParameters, fileContents, payload, 0);
            }

            throw e;
        }
    }

    protected InputStream internalExecuteCommand(OAuthToken oAuth, HttpMethodBase httpVerb, String path, KV[] queryParameters, KV[] bodyParameters, File fileContents, String payload, int retry) throws RESTException, IOException {

        StringBuilder pathBuilder = new StringBuilder();
        if (!path.startsWith("http")) pathBuilder.append(serverURL);
        pathBuilder.append(path);
        if (oAuth != null) pathBuilder.append("?api_key=").append(apiKey);
        if (queryParameters != null) {
            for (KV queryParameter : queryParameters) {
                if (queryParameter == null)
                    continue;

                pathBuilder.append("&").append(queryParameter.key()).append("=").append(queryParameter.value());
            }
        }

        log.debug("--pathBuilder.toString()=" + pathBuilder.toString());
        httpVerb.setPath(pathBuilder.toString());
        httpVerb.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

        if (oAuth == null) {
            httpVerb.setRequestHeader("Authorization", "Basic " + getEncodedCredentials(client, secret));
        } else {
            httpVerb.setRequestHeader("Authorization", oAuth.getType() + " " + oAuth.getToken());
        }


        if (bodyParameters != null || fileContents != null || payload != null) {

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

            if (payload != null) {
                httpVerb.setRequestHeader("Content-Type", "application/xml; charset=UTF-8");
                complexHttpVerb.setRequestEntity(new StringRequestEntity(payload, "application/xml", "UTF-8"));
            }


        }


        getClient().executeMethod(httpVerb);

        if (httpVerb.getStatusCode() == 401) {
            throw new RESTException("API call failed. Path:" + pathBuilder.toString(), httpVerb.getStatusCode(), httpVerb.getResponseBodyAsString(), httpVerb.getResponseContentLength());
        } else if (httpVerb.getStatusCode() != 200) {
            if (++retry <= _MAX_RETRY) {
                log.warn("ESB CALL FAILED! (Retrying " + retry + " of " + _MAX_RETRY + ") [code:" + httpVerb.getStatusCode() + " Path:" + pathBuilder.toString() + "]");
                sleep(5);
                return internalExecuteCommand(oAuth, httpVerb, path, queryParameters, bodyParameters, fileContents, payload, retry);
            } else {

                throw new RESTException("API call failed. Path:" + pathBuilder.toString(), httpVerb.getStatusCode(), httpVerb.getResponseBodyAsString(), httpVerb.getResponseContentLength());
            }
        }

        return httpVerb.getResponseBodyAsStream();

    }

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e2) {
        }

    }

    private static String detailErrorString(HttpMethodBase httpRequest, String payload, File fileContents) throws IOException {

        StringBuilder details = new StringBuilder();
        details.append("\n STATUS: ").append(httpRequest.getStatusCode());
        details.append("\n RESPONSE BODY: ").append(httpRequest.getResponseBodyAsString());
        details.append("\n REQUEST PATH: ").append(httpRequest.getPath());
        if (fileContents != null) details.append("\n FILE PAYLOAD: ").append(FileUtils.loadFileAsString(fileContents));
        if (payload != null) details.append("\n CONTENT PAYLOAD: ").append(payload);

        for (Header header : httpRequest.getRequestHeaders()) {
            details.append("\n REQUEST HEADER: ").append(header.getName()).append(" = ").append(header.getValue());
        }

        return details.toString();
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
