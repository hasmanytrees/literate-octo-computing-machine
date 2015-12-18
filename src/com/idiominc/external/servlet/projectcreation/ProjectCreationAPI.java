package com.idiominc.external.servlet.projectcreation;

import com.idiominc.external.AISUtils;
import com.idiominc.external.Utils;
import com.idiominc.external.json.JSONArray;
import com.idiominc.external.json.JSONObject;
import com.idiominc.external.config.Config;
import com.idiominc.external.json.JSONValue;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSObject;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.review.WSQualityModel;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSTask;
import com.idiominc.wssdk.workflow.WSWorkflow;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import java.io.*;

/**
 * Created by bslack on 10/2/15.
 */
public class ProjectCreationAPI extends HttpServlet {

    private final static String _PROCESS_ISL = "ISL";
    private final static String _PROCESS_TRANSLATION = "Translation";
    private final static String _PROCESS_TRANSCRIPTION = "Transcription";

    private final static String _B2S = "Beneficiary To Supporter";
    private final static String _S2B = "Supporter To Beneficiary";

    protected void doPost(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        final Exception[] eRef = new Exception[1];

        try {

            httpServletResponse.setContentType("application/json");
            WSContextManager.runWithToken("2", new WSRunnable() {

                public boolean run(WSContext context) {
                    try {

                        validate(context, httpServletRequest);

                        asJSON(createProject(
                                context,
                                getXMLPayload(httpServletRequest)
                        )).writeJSONString(httpServletResponse.getWriter());

                        return true;

                    } catch (Exception e) {
                        eRef[0] = e;
                        return false;
                    }
                }
            });

            if (eRef[0] != null)
                throw eRef[0];

        } catch (Exception e) {

            if (e instanceof AuthorizationException) {
                httpServletResponse.setStatus(401);
            } else {
                httpServletResponse.setStatus(400);
            }

            try {
                JSONObject msg = new JSONObject();
                msg.put("Reason", e.getMessage());
                msg.writeJSONString(httpServletResponse.getWriter());

            } catch (IOException e2) {
                e.printStackTrace();
            }
        }
    }

    public static void main3(String[] args) throws Exception {

        HttpClient httpClient = new HttpClient();
        PostMethod httpPost = new PostMethod("http://localhost:9091/ws104/compassion-api/v1/create_project");
        httpPost.setRequestEntity(new StringRequestEntity(
                Utils.loadFileAsString(new File(
                        "C:/AIS/Content/SDL Client/Projects/1593_compassion/Source-English/SupporterBeneficiaryCommunication.xml")),
                "application/xml",
                "UTF-8")
        );
        httpClient.executeMethod(httpPost);
        System.out.println("response=" + httpPost.getResponseBodyAsString());

    }

    protected WSProjectGroup createProject(WSContext context, String xmlPayload) throws Exception {

        ParsedPayload parsedPayload = new ParsedPayload(
                context,
                xmlPayload
        );

        return createProject(
                context,
                parsedPayload
        );
    }

    private WSProjectGroup createProject(
            WSContext context,
            ParsedPayload parsedPayload) throws Exception {


        if (context.getAisManager().getNode("/Remote Content") == null) {
            throw new IOException("The default Remote Content mount does not exist!");
        }

        // prepare data
        WSLocale originalSrcLocale = parsedPayload.getSourceLocale();
        WSLocale translationTgtLocale = parsedPayload.getTargetLocales()[0];
        // direct source is used for use with manual translation only
        WSLocale directSrcLocale = parsedPayload.getSourceLocale();
        WSLocale intermediaryLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context));
        WSLocale[] targetLocales = new WSLocale[]{intermediaryLocale};

        String processRequired = parsedPayload.getProcessRequired();
        String direction = parsedPayload.getDirection();
        WSWorkflow targetWorkflow = parsedPayload.getWorkflow();

        String workgroupName = parsedPayload.getWorkgroupName();
        WSWorkgroup workgroup = parsedPayload.getWorkgroup();

        String workflowOverrideName = null;

        boolean secondStepProjectRequired = false;
        boolean qcNotRequiredOverride = false;

        // Source/Target locale exception handling
        // Same language pair
        if(originalSrcLocale.getName().equals(translationTgtLocale.getName())) {
            // handle same language pair logic (English to English)
            targetLocales = new WSLocale[]{translationTgtLocale};
            secondStepProjectRequired = false;
            qcNotRequiredOverride = true;
            if(direction.equals(_B2S)) {
                // B2S: Create one-step project for FO for content-check only
                if(processRequired.equals(_PROCESS_TRANSLATION)) {
                    // handle manual translation
                } else {
                    // TODO: unexpected;
                }
            } else {
                // S2B: Create one-step project for FO for content-check only:
                workgroupName = parsedPayload.getFOWorkgroupName();
                workgroup = context.getUserManager().getWorkgroup(workgroupName);
                if(processRequired.equals(_PROCESS_ISL)) {
                    // handle ISL
                } else if(processRequired.equals(_PROCESS_TRANSCRIPTION)) {
                    // handle manual transcription
                } else {
                    // handle manual translation
                }

                //if GP has opted out
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    // do something
                }
            }
        } else if(originalSrcLocale.getName().equals(intermediaryLocale.getName())) {
            // original source is English (English to Spanish)
            targetLocales = new WSLocale[]{intermediaryLocale};
            if(direction.equals(_B2S)) {
                secondStepProjectRequired = true;
                qcNotRequiredOverride = true;
                workflowOverrideName = "Compassion Translation and Review Workflow-TwoStep - Step 2MTP";
                // B2S: Create two-step project, where FO will do content-check only, and GP will do manual translation
                if(processRequired.equals(_PROCESS_TRANSLATION)) {
                    // handle manual translation
                } else {
                    // TODO: unexpected;
                }
            } else {
                // S2B: Create one-step project, where FO will do manual translation
                targetLocales = new WSLocale[]{translationTgtLocale};
                workgroupName = parsedPayload.getFOWorkgroupName();
                workgroup = context.getUserManager().getWorkgroup(workgroupName);
                secondStepProjectRequired = false;
                qcNotRequiredOverride = false;

                if(processRequired.equals(_PROCESS_ISL)) {
                    // handle ISL
                    //directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion ISL Translation and Review Workflow-TwoStep - Step 2");
                } else if(processRequired.equals(_PROCESS_TRANSCRIPTION)) {
                    // handle manual transcription
                } else {
                    // handle manual translation
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2MTP");
                }

                //if GP has opted out
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    // do something
                }
            }
        } else if(translationTgtLocale.getName().equals(intermediaryLocale.getName())) {
            // final translation target locale is English (Spanish to English)
            targetLocales = new WSLocale[]{intermediaryLocale};
            secondStepProjectRequired = false;
            qcNotRequiredOverride = false;
            if(direction.equals(_B2S)) {
                // B2S: Create one-step project, where FO will translate to English and return with English translated text
                if(processRequired.equals(_PROCESS_TRANSLATION)) {
                    // handle manual translation
                    //directSrcLocale = intermediaryLocale;
                    directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-OneStep-MTP");
                    //targetLocales = new WSLocale[]{context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct")};
                    //targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2MTP");
                } else {
                    // TODO: unexpected;
                }
            } else {
                // S2B: Two-step process where GP will translate to English and FO will review English for content
                targetLocales = new WSLocale[]{intermediaryLocale};
                secondStepProjectRequired = true;
                if(processRequired.equals(_PROCESS_ISL)) {
                    // handle ISL
                } else if(processRequired.equals(_PROCESS_TRANSCRIPTION)) {
                    // handle manual transcription
                } else {
                    // handle manual translation
                    directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                }

                //if GP has opted out
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    directSrcLocale = intermediaryLocale;
                    //targetLocales = new WSLocale[]{context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct")};
                    targetLocales = new WSLocale[]{context.getUserManager().getLocale(Config.getIntermediaryLocale(context))};
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2");
                    workgroupName = parsedPayload.getFOWorkgroupName();
                    workgroup = context.getUserManager().getWorkgroup(workgroupName);

                }
            }
        } else {
            // otherwise this is standard two-step process; Spanish to French
            targetLocales = new WSLocale[]{intermediaryLocale};
            secondStepProjectRequired = true;
            if(direction.equals(_B2S)) {
                // B2S: Create two-step project, where FO will translate to English, and GP will translate to final lang
                if(processRequired.equals(_PROCESS_TRANSLATION)) {
                    // handle manual translation
                    directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");

                } else {
                    // TODO: unexpected;
                    directSrcLocale = parsedPayload.getSourceLocale();
                }
            } else {
                // S2B: Create two-step project, where GP will translate to English, and FO will translate to final lang
                if(processRequired.equals(_PROCESS_ISL)) {
                    // handle ISL
                    directSrcLocale = parsedPayload.getSourceLocale();
                } else if(processRequired.equals(_PROCESS_TRANSCRIPTION)){
                    // handle manual transcription
                    directSrcLocale = parsedPayload.getSourceLocale();
                } else {
                    // handle manual translation
                    directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                }

                //if GP has opted out
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    directSrcLocale = intermediaryLocale;
                    targetLocales = parsedPayload.getTargetLocales();
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2");
                    workgroupName = parsedPayload.getFOWorkgroupName();
                    workgroup = context.getUserManager().getWorkgroup(workgroupName);

                }
            }
        }


//        //TODO: Work-in-progress to do initial validation of "Manual translation process";
//        // - Require further update to handle trnaslation process requested, from ISL, Translation or Transcription,
//        // - Following section is for testing the Manual translation process
//        if(processRequired != null && processRequired.equals(_PROCESS_ISL)) {
//            directSrcLocale = parsedPayload.getSourceLocale();
//            if(directSrcLocale.getName().equals(intermediaryLocale.getName())) {
//                directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
//            }
//        } else if(processRequired != null && processRequired.equals(_PROCESS_TRANSCRIPTION)) {
//            directSrcLocale = parsedPayload.getSourceLocale();
//        } else {
//            // manual translation process
//            directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
//        }
//
//
//        // For target locale setup, use intermediary locale, default value: English (United States)
//        // Target language in the XML payload will be used by second-step project
//        WSLocale[] targetLocales = new WSLocale[]{intermediaryLocale};
//
//        //TODO: Confirm logic for when GP has opted out
//        if (parsedPayload.getDirection() != null && parsedPayload.getDirection().equals("Supporter To Beneficiary")) {
//            if(parsedPayload.getGPOptInForTranslation().equals("false")) {
//                directSrcLocale = intermediaryLocale;
//                targetLocales = parsedPayload.getTargetLocales();
//                targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2");
//            }
//        }


        WSNode n = AISUtils.createContentInAIS(
                context,
                directSrcLocale,
                parsedPayload.getTargetLocales(),
                //parsedPayload.getWorkgroupName(),
                workgroupName,
                parsedPayload.getContent()
        );

        AISUtils.setupTargetLocaleFolders(context, parsedPayload.getSourceLocale(), targetLocales);

        WSProjectGroup pg = context.getWorkflowManager().createProjectGroup(
                parsedPayload.getProjectName(),
                parsedPayload.getProjectDescription(),
                //parsedPayload.getWorkgroup(),
                workgroup,
                targetLocales,
                new WSNode[]{n},
                targetWorkflow,
                0,
                null
        );

        if (pg == null || pg.getProjects().length != parsedPayload.getTargetLocales().length) {
            if (pg != null) {
                for (WSProject p : pg.getProjects()) {
                    p.cancel("Invalid project was created!");
                }
            }

            throw new IOException("Failed to create projects!");
        }

        // store the second step project preference
        for(WSProject p : pg.getProjects()) {
            p.setAttribute("secondStepProjectRequired", Boolean.toString(secondStepProjectRequired));
            p.setAttribute("qcNotRequiredOverride", Boolean.toString(qcNotRequiredOverride));
            p.setAttribute("workflowOverride", workflowOverrideName);

            //set the default quality model
            WSQualityModel qModel = context.getReviewManager().getQualityModel("Default QC Model");
            if(qModel != null) {
                p.setQualityModel(qModel);
            }
        }

        return pg;
    }

    private String getXMLPayload(HttpServletRequest httpServletRequest) throws IOException {
        return getBody(httpServletRequest);
    }

    private String getBody(HttpServletRequest request) throws IOException {

        StringBuffer jb = new StringBuffer();
        String line = null;
        boolean firstLine = true;

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
        while ((line = reader.readLine()) != null) {

            /**
             * This fixes a known Java issue where the Byte Order Mark is appended to the beginning on the
             * first line, this removes the UTF-8 BOM
             */
            if (firstLine) {
                line = removeUTF8BOM(line);
                firstLine = false;
            }

            jb.append(line);
        }

        return jb.toString();
    }

    private static String removeUTF8BOM(String s) {
        String UTF8_BOM = "\uFEFF";

        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    protected void validate(WSContext context, HttpServletRequest httpServletRequest) throws AuthorizationException {
        try {
            // allow for comma-separated list of authorized user accounts
            String authorizedNTLMUserStr = Config.getNTLMUser(context);
            String[] authorizedNTLMUsers = authorizedNTLMUserStr.split(",");
            boolean authorizedNTLMUserFound = false;
            for (String authorizedNTLMUser : authorizedNTLMUsers) {
                if (httpServletRequest.getUserPrincipal() != null && httpServletRequest.getUserPrincipal().getName().equals(authorizedNTLMUser)) {
                    authorizedNTLMUserFound = true;
                }
            }

            if (!authorizedNTLMUserFound)
                throw new AuthorizationException("Role is not authorized Required=" + authorizedNTLMUserStr + " Found=" + httpServletRequest.getUserPrincipal());

        } catch (IOException e) {
            throw new AuthorizationException(e.getMessage());
        }
    }

    private class AuthorizationException extends Exception {
        AuthorizationException(String msg) {
            super(msg);
        }
    }

    private class ParsedPayload {

        private WSContext context;
        private Document xmlPayload;
        private String contents;
        private String direction;
        private String workgroupName;

        private File f;

        public ParsedPayload(WSContext context, String xmlPayload) throws SAXException, ParserConfigurationException, IOException {

            this.context = context;
            this.contents = xmlPayload;
            this.xmlPayload = Utils.load(new InputSource(new StringReader(xmlPayload)));
        }

        public void setContent(String content) throws SAXException, ParserConfigurationException, IOException {
            this.contents = content;
            this.xmlPayload = Utils.load(new InputSource(new StringReader(content)));
        }

        public File getContent() throws IOException, XPathException {
            if (f == null) {
                f = File.createTempFile("content_" + Utils.getField(xmlPayload, "//CompassionSBCId"), ".xml");
                Utils.getStringAsFile(f, contents, "UTF-8");
            }

            return f;
        }

        public String getProjectName() throws XPathException {
            String commKitTypes = getCommunicationKitTypes();
            return
                    //Utils.getField(xmlPayload, "//SBCTypes") +
                    commKitTypes +
                    " #" + Utils.getField(xmlPayload, "//CompassionSBCId") +
                    " [" + getProcessRequired() + "]";
        }


        public String getProcessRequired() throws XPathException {
            // Identify if any text is found in the original text field, and if so, return with ISL-process
            String origText = Utils.getField(xmlPayload, "//OriginalText");
            if(origText != null && !origText.equals("")) {
                // original text found in xml payload; return with ISL workflow process!
                return _PROCESS_ISL;
            }

            return Utils.getField(xmlPayload, "//SDLProcessRequired");
        }

        public String getDirection() throws XPathException {
            if (direction == null) {
                direction = Utils.getField(xmlPayload, "//Direction");
            }

            return direction;
        }

        // Return relationship type for comm kit type for S2B;
        // Return sbc type for comm kit type for B2S.
        public String getCommunicationKitTypes() throws XPathException {
            StringBuilder ret = new StringBuilder();

            String dir = getDirection();
            if(dir != null && dir.equals("Supporter To Beneficiary")) {
                ret.append(Utils.getField(xmlPayload, "//RelationshipType")).append(" Letter");
            } else {
                for (String type : Utils.getFields(xmlPayload, "//SBCTypes")) {
                    ret.append(type).append(" ");
                }
            }

            return ret.toString();
        }

        public String getProjectDescription() throws XPathException {
            StringBuilder ret = new StringBuilder();
            ret.append(getDirection()).append(" ");
            ret.append(getCommunicationKitTypes());

            return ret.toString();
        }

        public WSLocale getSourceLocale() throws XPathException {
            return validate(context.getUserManager().getLocale(Utils.getField(xmlPayload, "//OriginalLanguage")));
        }

        public WSLocale[] getTargetLocales() throws XPathException {
            return new WSLocale[]{
                    validate(context.getUserManager().getLocale(Utils.getField(xmlPayload, "//TranslationLanguage"))),
            };
        }

        public String getGPOptInForTranslation() throws XPathException {
            return Utils.getField(xmlPayload, "//GlobalPartner/OptInForLanguageTranslation");
        }

        public String getFOWorkgroupName() throws XPathException {
            String foName = Utils.getField(xmlPayload, "//FieldOffice/Name");
            return "FO_" + foName;
        }

        public String getWorkgroupName() throws XPathException {
            if (workgroupName != null) {
                return workgroupName;
            }

            if (getDirection() != null && getDirection().equals("Supporter To Beneficiary")) {
                // get workgroup name for given GP
                String gpName = Utils.getField(xmlPayload, "//GlobalPartner/Id");
                workgroupName = "GP_" + gpName;
            } else if (getDirection() != null && getDirection().equals("Beneficiary To Supporter")) {
                // get workgroup name for given FO
                String foName = Utils.getField(xmlPayload, "//FieldOffice/Name");
                workgroupName = "FO_" + foName;
            } else {
                // unsupported direction; default workgroup?
                workgroupName = "default";
            }

            return workgroupName;
        }

        public WSWorkgroup getWorkgroup() throws XPathException {
            return validate(context.getUserManager().getWorkgroup(getWorkgroupName()));
        }

        public WSWorkflow getWorkflow() throws XPathException, IOException {
            //Dynamitcally fetch workflowname based on <SDLProcessRequired> flag in the payload
            String processRequired = getProcessRequired();
            String workflowName;

            switch (processRequired) {
                case _PROCESS_ISL:
                    workflowName = Config.getWorkflowNameISL(context);
                    break;
                case _PROCESS_TRANSLATION:
                    workflowName = Config.getWorkflowNameTranslation(context);
                    break;
                case _PROCESS_TRANSCRIPTION:
                    workflowName = Config.getWorkflowNameTranscription(context);
                    break;
                default:
                    // Unknown; Use translation workflow as default
                    workflowName = Config.getWorkflowNameTranslation(context);
                    break;
            }

            return validate(context.getWorkflowManager().getWorkflow(workflowName));
        }
    }

    private <T extends WSObject> T validate(T wsObject) {
        if (wsObject == null)
            throw new IllegalArgumentException("Invalid payload parameters. Object does not exist.");
        return wsObject;

    }


    private JSONObject asJSON(WSProjectGroup pg) {
        JSONObject ret = new JSONObject();

        JSONArray projectList = new JSONArray();
        ret.put("projects", projectList);

        for (final WSProject p : pg.getProjects()) {
            JSONObject projectJSON = new JSONObject() {{
                put("id", p.getId());
                put("tasks", new JSONArray() {{
                    add(new JSONObject() {{
                        put("id", new JSONArray() {{
                            for (final WSTask t : p.getActiveTasks()) {
                                add(t.getId());
                            }
                        }});
                    }});
                }});
            }};

            projectList.add(projectJSON);
        }

        return ret;
    }


    public static void main2(String[] args) throws Exception {
        WSContextManager.runWithToken("2", new WSRunnable() {

            public boolean run(WSContext context) {

                ProjectCreationAPI test = new ProjectCreationAPI();
                try {
                    test.asJSON(test.createProject(
                            context,
                            Utils.loadFileAsString(new File("C:/AIS/Content/SDL Client/Projects/1593_compassion/Source-English/SupporterBeneficiaryCommunication.xml"))
                    )).writeJSONString(new OutputStreamWriter(System.out));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new WSRuntimeException(e); //todo
                }
            }
        });

    }


}
