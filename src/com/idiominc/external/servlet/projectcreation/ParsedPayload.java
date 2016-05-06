package com.idiominc.external.servlet.projectcreation;

import com.idiominc.external.Utils;
import com.idiominc.external.config.Config;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSObject;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.WSWorkflow;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class ParsedPayload {

    private WSContext context;
    private Document xmlPayload;
    private String contents;
    private String direction;
    private String workgroupName;

    private File f;

    public ParsedPayload(WSContext context, String xmlPayload)
            throws SAXException, ParserConfigurationException, IOException {

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
            return CIProjectConfig._PROCESS_ISL;
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

        StringBuilder ret;

        ret = new StringBuilder();
        ret.append(getDirection()).append(" ");
        ret.append(getCommunicationKitTypes());

        return ret.toString();
    }

    public WSLocale getSourceLocale() throws XPathException {
        String originalLanguage = Utils.getField(xmlPayload, "//OriginalLanguage");
        return validate(context.getUserManager().getLocale(originalLanguage),
                "[" + Utils.getField(xmlPayload, "//CompassionSBCId") + "]" +
                        "Original Language:" + originalLanguage);
    }

    public WSLocale[] getTargetLocales() throws XPathException {
        String translationLanguage = Utils.getField(xmlPayload, "//TranslationLanguage");
        return new WSLocale[]{
                validate(context.getUserManager().getLocale(translationLanguage),
                        "[" + Utils.getField(xmlPayload, "//CompassionSBCId") + "]" +
                                "Translation Language:" + translationLanguage),
        };
    }

    public String getGPOptInForTranslation() throws XPathException {
        return Utils.getField(xmlPayload, "//GlobalPartner/OptInForLanguageTranslation");
    }

    public String getFOWorkgroupName() throws XPathException {
        String foName = Utils.getField(xmlPayload, "//FieldOffice/Name");
        return "FO_" + foName;
    }

    public String getGPWorkgroupName() throws XPathException {
        String gpName = Utils.getField(xmlPayload, "//GlobalPartner/Id");
        return "GP_" + gpName;
    }

    public String getWorkgroupName() throws XPathException {

        if (workgroupName != null) {
            return workgroupName;
        }

        // Get the "direction" of the letter
        String letterDirection = getDirection();

        if(letterDirection != null) {

            // Set the default workgroupName based on letter type
            switch(letterDirection) {
                case "Supporter To Beneficiary":
                    workgroupName = getGPWorkgroupName();
                    break;
                case "Beneficiary To Supporter":
                    workgroupName = getFOWorkgroupName();
                    break;
                case "Third Party Letter":
                    workgroupName = getFOWorkgroupName();
                    break;
                default:
                    workgroupName = "default";
                    break;
            }
        }

        return workgroupName;
    }

    public WSWorkgroup getWorkgroup() throws XPathException {
        String workgroupName = getWorkgroupName();
        return validate(context.getUserManager().getWorkgroup(workgroupName),
                "[" + Utils.getField(xmlPayload, "//CompassionSBCId") + "]" +
                        "Workgroup:" + workgroupName);
    }

    public WSWorkflow getWorkflow() throws XPathException, IOException {
        //Dynamically fetch workflowname based on <SDLProcessRequired> flag in the payload
        String processRequired = getProcessRequired();
        String workflowName;

        if(processRequired.equals(CIProjectConfig._PROCESS_ISL))
            workflowName = Config.getWorkflowNameISL(context);
        else if(processRequired.equals(CIProjectConfig._PROCESS_TRANSLATION))
            workflowName = Config.getWorkflowNameTranslation(context);
        else if(processRequired.equals(CIProjectConfig._PROCESS_TRANSCRIPTION))
            workflowName = Config.getWorkflowNameTranscription(context);
        else
            workflowName = Config.getWorkflowNameTranslation(context);
        return validate(context.getWorkflowManager().getWorkflow(workflowName),
                "[" + Utils.getField(xmlPayload, "//CompassionSBCId") + "]" +
                        "Workflow:" + workflowName);
    }

    private <T extends WSObject> T validate(T wsObject, String objectName) {
        if (wsObject == null)
            throw new IllegalArgumentException("Invalid payload parameters. Object does not exist: " + objectName);
        return wsObject;
    }
}

