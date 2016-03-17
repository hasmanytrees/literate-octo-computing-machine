package com.idiominc.external.servlet.projectcreation;

//internal dependencies
import com.idiominc.external.AISUtils;
import com.idiominc.external.Utils;
import com.idiominc.external.json.JSONArray;
import com.idiominc.external.json.JSONObject;
import com.idiominc.external.config.Config;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSObject;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.review.WSQualityModel;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSTask;
import com.idiominc.wssdk.workflow.WSWorkflow;

//apache
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

//sax and DOM
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//J2EE and JAVA
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.*;

/**
 * External component responsible for listening for project creation request and creating projects in WorldServer.
 *
 * @author SDL Professional Services
 */
public class NewProjectCreationAPI extends HttpServlet {

    private final static String _PROCESS_ISL = "ISL";
    private final static String _PROCESS_TRANSLATION = "Translation";
    private final static String _PROCESS_TRANSCRIPTION = "Transcription";
    private final static String _B2S = "Beneficiary To Supporter";

    //log
    private final Logger log = Logger.getLogger(NewProjectCreationAPI.class);

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
                        log.error("Project creation error:", e);
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
                log.error("Project creation error - ", e);
                JSONObject msg = new JSONObject();
                msg.put("Reason", e.getMessage());
                msg.writeJSONString(httpServletResponse.getWriter());

            } catch (IOException e2) {
                e.printStackTrace();
            }
        }
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
        WSLocale[] targetLocales;

        String processRequired = parsedPayload.getProcessRequired();
        String direction = parsedPayload.getDirection();
        WSWorkflow targetWorkflow = parsedPayload.getWorkflow();

        String workgroupName = parsedPayload.getWorkgroupName();
        WSWorkgroup workgroup = parsedPayload.getWorkgroup();

        String workflowOverrideName = null;

        boolean secondStepProjectRequired;
        boolean qcNotRequiredOverride = false;

        // capture info required to perform content clean-up at delivery time
        // This will capture true/false for each of the following three fields
        // OriginalText, English Translated Text, Final Translated Text
        // If True, then the content will be sent back; if false, then empty content will be sent back for each corresponding field
        // "[true|false]|[true|false]|[true|false]"
        String returnTextRequirements;

        String LanguageExceptionType = "";

        // Same language pair
        if(originalSrcLocale.getName().equals(translationTgtLocale.getName())) {
            // handle same language pair logic (English to English)

            targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-SameLang");
            if(direction.equals(_B2S)) {
                /*
                 * a. FO Translator only does the Content Check passes it
                 * forward or reject (for uncorrectable errors).
                 * Correctable errors cannot be corrected by the Translator
                 * b. No Composition required
                 * c. GP receives the Original Letter Image
                 */
                if(processRequired.equals(_PROCESS_TRANSLATION)) {
                    targetLocales = new WSLocale[]{translationTgtLocale};
                    secondStepProjectRequired = false;
                    qcNotRequiredOverride = true;
                } else {
                    throw new Exception("Same language pair. Direction is " + _B2S
                                        + ". Requested proecess is " + processRequired + " which is not supported" );
                }
                if(parsedPayload.getGPOptInForTranslation().equals("true")) {
                   /*
                     GP uses SDL
                     SDL can trigger a project for GP Translator to do the transcription
                     - Deferred post R4 [Daniel to provide impact estimate]
                   */
                }

                /* For B2S, output text requirements are:
                Original Text - Blank
                English Text - Blank
                Final Translated Text - Blank
                --> Updated January 12, 2016
                Original Text - Blank
                English Text - English
                Final Translated Text - English
                 */
                returnTextRequirements = "false|true|true";
            } else {
                // S2B: Create one-step project for FO for content-check only:
                /*
                 *  a. GP send the Original Letter without Content Check
                 *  b. FO receives just the letter image and does the Content Check and pass it forward or reject (for uncorrectable errors). Correctable errors cannot be corrected by the Translator
                 *  c. No Composition required
                 *
                 */
                targetLocales = new WSLocale[]{translationTgtLocale};
                secondStepProjectRequired = false;
                qcNotRequiredOverride = true;
                workgroupName = parsedPayload.getFOWorkgroupName();
                workgroup = context.getUserManager().getWorkgroup(workgroupName);
                //workflow will be determined dynamically based on ISL versus translation process

                //if GP has opted out, does not use SDL
                if (parsedPayload.getGPOptInForTranslation().equals("false")) {
                    /*
                      No difference im process whether or not GP uses SDL
                     *
                     */
                }

                //if ISL content
                if(processRequired.equals(_PROCESS_ISL)) {
                    /*
                    Original Text - GP Language
                    English Text - Blank
                    Final Translated Text - Blank                     */
                    returnTextRequirements = "true|false|false";
                } else {
                    /*
                    Original Text - Blank
                    English Text - Blank
                    Final Translated Text - Blank
                    --> Updated January 12, 2016
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - English
                     */
                    returnTextRequirements = "false|true|true";
                }
            }
        } else if(originalSrcLocale.getName().equals(intermediaryLocale.getName())) {
            // original source is English (English to Spanish)
            targetLocales = new WSLocale[]{intermediaryLocale};
            if(direction.equals(_B2S)) {
                /*
                 *a. FO Translator only does the Content Check pass it forward or reject (for uncorrectable errors).
                 * Correctable errors cannot be corrected by the Translator
                 * b. GP Translator does the English to Final Language translation using the Manual Translation Process
                 * c. XMPie composes the letter with the final translation
                 * d. GP receives the Composed Letter and Final language blob
                 */

                // Create two-step project, where FO will do content-check only,
                // and GP will do manual translation
                secondStepProjectRequired = true;
                qcNotRequiredOverride = true;
                workflowOverrideName = "Compassion Translation and Review Workflow-TwoStep - Step 2MTP";

                //First step project will reassign using for same-lang pair
                //Use <Target>-Direct as source for second step project

                if(!processRequired.equals(_PROCESS_TRANSLATION)) {
                    throw new Exception("Different language pair - Type 1, B2S. Expected process to be "
                                        + _PROCESS_TRANSLATION + " but it is " + processRequired);
                }

                    /*
                    Original Text - Blank
                    English Text - Blank
                    Final Translated Text - GP Language
                     */
                returnTextRequirements = "false|false|true";

                //if GP has opted out - do not use SDL
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    /*
                     a. FO Translator only does the Content Check pass it forward or reject (for uncorrectable errors). Correctable errors cannot be corrected by the Translator
                     b. GP receives just the Original letter image through the Handshake
                     c. GP Translator does the English to Final Language translation using
                     the English Letter Image through THEIR (non-SDL) Translation tool
                     */
                    secondStepProjectRequired = false;

                    /*
                    Original Text - Blank
                    English Text - Blank
                    Final Translated Text - Blank
                     */
                    returnTextRequirements = "false|false|false";
                }
                
            } else {
                /*
                  S2B
                  a. FO receives the letter image
                  b. FO Translator will do the English to FO Language using the
                    SDL Manual Translation Process and also do the Content Check
                  c. XMPie composes the letter with the final translation
                */
                //Create one-step project, where FO will do manual translation
                //Use <Target>-Direct as sources
                targetLocales = new WSLocale[]{translationTgtLocale};
                directSrcLocale = context.getUserManager().getLocale(translationTgtLocale.getName() + "-Direct");
                workgroupName = parsedPayload.getFOWorkgroupName();
                workgroup = context.getUserManager().getWorkgroup(workgroupName);
                secondStepProjectRequired = false;
                qcNotRequiredOverride = false;
                targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2MTP");

                    /*
                    Original Text - Blank
                    English Text - Blank
                    Final Translated Text - FO Language
                     */
                returnTextRequirements = "false|false|true";

                if(processRequired.equals(_PROCESS_ISL)) {
                    /*
                      a. GP sends the ISL letter Images and the text blob
                      b. FO receives Text blob does the Content Check and English to FO Language
                         translation using CAT
                      c. XMPie composes the letter with the final translation
                     */
                     targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion ISL Translation and Review Workflow-TwoStep - Step 2");
                    directSrcLocale = originalSrcLocale;

                    /*
                    Original Text - English
                    English Text - English
                    Final Translated Text - Fo Language
                     */
                    returnTextRequirements = "true|true|true";
                }

                //no difference whether or not GP opted out in type 1 for S2B
            }
        } else if(translationTgtLocale.getName().equals(intermediaryLocale.getName())) {
            // Different language pair - type 2
            // final translation target locale is English (Spanish to English)
            if(direction.equals(_B2S)) {
                /*
                  a. FO does the Content Check and FO Language to English translation using the Manual Translation Process
                  b. XMPie composes the letter with the final translation
                  c. GPA to receive the Original letter image, Composed letter and the English text blob
                 */
                 // Create one-step project, where FO will translate to English and
                 // return with English translated text (populated in <TranslatedText> fields

                secondStepProjectRequired = false;
                qcNotRequiredOverride = false;
                directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                targetLocales = new WSLocale[]{intermediaryLocale};
                targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-OneStep-MTP");
                if(!processRequired.equals(_PROCESS_TRANSLATION)) {
                    throw new Exception("Different language pair - Type 2, B2S. Expected process to be "
                                        + _PROCESS_TRANSLATION + " but it is " + processRequired);
                }
                //no difference whether or not GP opted out in type 2 for B2S

                    /*
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - English
                     */
                returnTextRequirements = "false|true|true";

            } else {
                /*
                  a. GP Translator will do the GP Language to English using the SDL CAT
                  b. GP sends the ISL letter Images and the text blob
                  c. FO receives Text blob, does the Content Check
                  d. XMPie composes the letter with the final translation
                */
                // Two-step process where GP will translate to English and
                // FO will review English for content
                qcNotRequiredOverride = false;
                targetLocales = new WSLocale[]{intermediaryLocale};
                secondStepProjectRequired = true;
                workflowOverrideName = "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B";
                if(processRequired.equals(_PROCESS_TRANSLATION)) {
                    /*
                      a. GP Translator will do the GP Language to English using SDL Manual Translation Process
                      b. FO receives the Original Letter Image and English text
                      c. FO does the Content Check
                      d. XMPie composes the letter with the English translation
                    */
                    directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                    //Second step project will use <Eng to Eng>
                    //Any content check changes are captured in <EnglishTranslatedText> fields and subsequently copied to <TranslatedText> field for proper return
                }

                //if GP has opted out and does not use SDL
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    /*
                       a. GP Translator will do the GP Language to English
                       b. FO receives the Original Letter Image and English text
                       c. FO does the Content Check
                       d. XMPie composes the letter with the English translation
                       **************ISL Letters************
                       a. GP Translator will do the GP Language to English
                       b. GP sends the ISL letter Images and the text blob
                       c. FO receives Text blob does the Content Check
                       d. XMPie composes the letter with the final translation

                       NB: From FO prospective, there will be no different process,
                       so same code for ISL or none-ISL below
                    */
                    qcNotRequiredOverride = true;
                    secondStepProjectRequired = false;
                    directSrcLocale = intermediaryLocale;
                    targetLocales = new WSLocale[]{context.getUserManager().getLocale(Config.getIntermediaryLocale(context))};
                    //targetLocales = new WSLocale[]{context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct")};
                    //targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2");
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
                    workgroupName = parsedPayload.getFOWorkgroupName();
                    workgroup = context.getUserManager().getWorkgroup(workgroupName);
                    //Any content check changes are captured in <EnglishTranslatedText> fields and subsequently copied to <TranslatedText> field for proper return
                }

                if(processRequired.equals(_PROCESS_ISL)) {
                    /*
                    Original Text - GP Language
                    English Text - English
                    Final Translated Text - English
                     */
                    returnTextRequirements = "true|true|true";
                } else {
                    /*
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - English
                     */
                    returnTextRequirements = "false|true|true";
                }
            }
        } else {
            // otherwise this is standard two-step process; Spanish to French (Type 3)
            if(direction.equals(_B2S)) {
                /*
                  a. FO does the Content Check and FO Language to English translation using the SDL Manual Translation Process
                  b. SDL triggers the GP Translation process
                  c. GP Translator does the English to GP Language translation using CAT
                    (GP Translator will have the ability to see the Original Letter Image)
                  d. XMPie composes the letter with the final translation
                  e. GP receives the Original Letter Image, Composed Letter Image and Final translated text
                */
                targetLocales = new WSLocale[]{intermediaryLocale};
                secondStepProjectRequired = true;
                directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");

                // B2S: Create two-step project, where FO will translate to English, and GP will translate to final lang
                if(processRequired.equals(_PROCESS_TRANSCRIPTION)) {
                    directSrcLocale = originalSrcLocale;
                }

                    /*
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - GP Language
                     */
                returnTextRequirements = "false|true|true";

                //if GP has opted out, does not use SDL
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    /*
                       a. FO does the Content Check and FO Language to English translation
                          using the SDL Manual Translation Process
                       b. GP receives the Original Letter Image and English text
                    */
                    secondStepProjectRequired = false;

                    /*
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - Blank
                     */
                    returnTextRequirements = "false|true|false";

                }

            } else {

                /*
                  a. GP sends the Orginal Letter Image
                  b. GP Translator does the GP Language to English translation using the SDL Manual Translation Process
                  c. SDL triggers the FO Translation process
                  d. FO does the Content Check and English to FO Language translation using the Auto-translation
                  e. XMPie composes the letter with the final translation
                  f. FO receives the Composed Letter
                  **************ISL Letters********************************************************
                  a. GP does the GP Language to English translation through their Translation tool
                  b. FO receives Original ISL Image and the English Text blob does the Content Check and English to FO Language translation using CAT
                  c. XMPie composes the letter with the final translation
                */
                // S2B: Create two-step project, where GP will translate to English,
                // and FO will translate to final lang
                targetLocales = new WSLocale[]{intermediaryLocale};
                secondStepProjectRequired = true;
                if(processRequired.equals(_PROCESS_ISL)) {
                    // handle ISL
                    directSrcLocale = parsedPayload.getSourceLocale();
                    /*
                    Original Text - GP Language
                    English Text - English
                    Final Translated Text - FO Language
                     */
                    returnTextRequirements = "true|true|true";
                } else if(processRequired.equals(_PROCESS_TRANSCRIPTION)){
                    // handle manual transcription
                    directSrcLocale = parsedPayload.getSourceLocale();
                    /*
                    Original Text - GP Language
                    English Text - English
                    Final Translated Text - FO Language
                     */
                    returnTextRequirements = "true|true|true";
                } else {
                    // handle manual translation
                    directSrcLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                    /*
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - FO Language
                     */
                    returnTextRequirements = "false|true|true";
                }

                //if GP has opted out, does not use SDL
                if(parsedPayload.getGPOptInForTranslation().equals("false")) {
                    LanguageExceptionType = "Type3_S2B_NoGP";
                   /*
                     a. GP sends the Orginal Letter Image and the English translation blob
                     b. SDL triggers the FO Translation process
                     c. FO does the Content Check and English to FO Language translation using the Auto-translation
                     d. XMPie composes the letter with the final translation
                     e. FO receives the Composed Letter
                      **************ISL Letters************
                     a. GP does the GP Language to English translation through their Translation tool
                     b. FO receives Original ISL Image and the English Text blob, does the Content Check and English to FO Language translation using CAT
                     c. XMPie composes the letter with the final translation
                    */
                    // no difference in the workflow for both ISL and non-ISL
                    directSrcLocale = intermediaryLocale;
                    targetLocales = parsedPayload.getTargetLocales();
                    targetWorkflow = context.getWorkflowManager().getWorkflow("Compassion Translation and Review Workflow-TwoStep - Step 2");
                    workgroupName = parsedPayload.getFOWorkgroupName();
                    workgroup = context.getUserManager().getWorkgroup(workgroupName);

                    if(processRequired.equals(_PROCESS_ISL)) {
                    /*
                    Original Text - GP Language
                    English Text - English
                    Final Translated Text - FO Language
                     */
                        returnTextRequirements = "true|true|true";
                    } else {
                    /*
                    Original Text - Blank
                    English Text - English
                    Final Translated Text - FO Language
                     */
                        returnTextRequirements = "false|true|true";
                    }
                }
            }
        }

        WSNode n = AISUtils.createContentInAIS(
                context,
                directSrcLocale,
                parsedPayload.getTargetLocales(),
                workgroupName,
                parsedPayload.getContent()
        );

        AISUtils.setupTargetLocaleFolders(context, parsedPayload.getSourceLocale(), targetLocales);

        WSProjectGroup pg = context.getWorkflowManager().createProjectGroup(
                parsedPayload.getProjectName(),
                parsedPayload.getProjectDescription(),
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
            p.setAttribute("electronicContent", Boolean.toString(parsedPayload.getProcessRequired().equals(_PROCESS_ISL)));
            //set the default quality model
            WSQualityModel qModel = context.getReviewManager().getQualityModel("Default QC Model");
            if(qModel != null) {
                p.setQualityModel(qModel);
            }
            p.setAttribute("returnTextRequirements", returnTextRequirements);
            p.setAttribute("LanguageExceptionType", LanguageExceptionType);
        }

        return pg;
    }

    private String getXMLPayload(HttpServletRequest httpServletRequest) throws IOException {
        return getBody(httpServletRequest);
    }

    private String getBody(HttpServletRequest request) throws IOException {

        StringBuffer jb = new StringBuffer();
        String line;
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

            if(processRequired.equals(_PROCESS_ISL))
                 workflowName = Config.getWorkflowNameISL(context);
            else if(processRequired.equals(_PROCESS_TRANSLATION))
                  workflowName = Config.getWorkflowNameTranslation(context);
            else if(processRequired.equals(_PROCESS_TRANSCRIPTION))
                  workflowName = Config.getWorkflowNameTranscription(context);
            else
                   workflowName = Config.getWorkflowNameTranslation(context);
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
                put("id", new Integer(p.getId()));
                put("tasks", new JSONArray() {{
                    add(new JSONObject() {{
                        put("id", new JSONArray() {{
                            for (final WSTask t : p.getActiveTasks()) {
                                add(new Integer(t.getId()));
                            }
                        }});
                    }});
                }});
            }};

            projectList.add(projectJSON);
        }

        return ret;
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
        System.out.println("response = " + httpPost.getResponseBodyAsString());

    }

    public static void main2(String[] args) throws Exception {
        WSContextManager.runWithToken("2", new WSRunnable() {
            public boolean run(WSContext context) {
                NewProjectCreationAPI test = new NewProjectCreationAPI();
                try {
                    test.asJSON(test.createProject(
                            context,
                            Utils.loadFileAsString(new File("C:/AIS/Content/SDL Client/Projects/1593_compassion/Source-English/SupporterBeneficiaryCommunication.xml"))
                    )).writeJSONString(new OutputStreamWriter(System.out));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });

    }

}
