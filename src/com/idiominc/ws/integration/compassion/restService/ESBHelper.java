package com.idiominc.ws.integration.compassion.restService;

import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.workflow.WSProject;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

/**
 * Helper class utilized by the SetESBStatus automatic action.
 *
 * @author SDL Professional Services
 */
public class ESBHelper {

    private final static String _B2S = "Beneficiary To Supporter";

    // Get Communication kit global ID
    public static String getId(WSAssetTask task) throws IOException {

        try {
            Document d = XML.load(task.getSourceAisNode().getFile());
            return XML.getField(d, "//CompassionSBCId");
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    // build status update XML payload
    public static String buildStatusXML(WSAssetTask task, KV... kvs) throws IOException {

        try {
            Document d = XML.load(task.getSourceAisNode().getFile());
            Document esbStatusDocument = newDocument();

            Element base = esbStatusDocument.createElement("CommunicationUpdates");

            addTo(d, base, "CompassionSBCId");
            addTo(d, base, "SourceSystem", "SDL");

            if (kvs != null) {
                for (KV kv : kvs) {
                    if (kv.key() == null || "".equals(kv.key().trim()))
                        continue;
                    addTo(d, base, kv.key(), kv.value());
                }
            }

            return XML.parseToString(base);
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    // build return XML payload
    public static String buildReturnKitXML(WSAssetTask task) throws IOException {

        try {

            Document d = XML.load(task.getTargetAisNode().getFile());
            addTo(d, "SBCGlobalStatus", "Translation and quality check complete");
            addTo(d, "SourceSystem", "SDL");
            addTo(d, "ReasonForRework", "");

            // also need to add the questions list to the return XML
            // Updated: 1/13/2016: The questions should be sent back only for S2B, not for B2S
            String direction = task.getProject().getAttribute("Direction");
            if(direction != null && !direction.equals(_B2S)) {
                String questionsList = task.getAttribute("questionAttr");
                if (questionsList != null && !questionsList.equals("")) {
                    addTo(d, "SupporterQuestions", questionsList);
                }
            }
            // set the last translator who did the translation -- Not yet ready
            addTo(d, "TranslatedBy", task.getProject().getAttribute("MostRecentTranslator"));

            // prep and clean up the return kit text fields
            returnKitTextFieldSetup(task.getProject(), d);

            return XML.parseToString(d);

        } catch (Exception e) {
            throw new IOException(e);
        }

    }


    // Clean up the return kit text fields based on field requirements as it was captured during project creation
    // This method handles removing unnecessary fields based on language-exception business requirements.
    private static void returnKitTextFieldSetup(WSProject project, Document d) throws XPathExpressionException, TransformerException {

        Logger log = Logger.getLogger(ESBHelper.class);

        String returnTextRequirementsStr = project.getAttribute("returnTextRequirements");

        if(returnTextRequirementsStr != null && !returnTextRequirementsStr.equals("")) {
            // parse the return text requirements
            String[] returnTextRequirementsStrArray = returnTextRequirementsStr.split("\\|");

            boolean origTextReqd = true;
            try {
                origTextReqd = Boolean.parseBoolean(returnTextRequirementsStrArray[0]);
            } catch (Exception e) {
                // ignore any error; default to true;
            }
            boolean englishTextReqd = true;
            try {
                englishTextReqd = Boolean.parseBoolean(returnTextRequirementsStrArray[1]);
            } catch (Exception e) {
                // ignore any error; default to true
            }
            boolean translatedTextReqd = true;
            try {
                translatedTextReqd = Boolean.parseBoolean(returnTextRequirementsStrArray[2]);
            } catch (Exception e) {
                // ignore any error; default to true
            }

            if(!origTextReqd) {
                NodeList nodeList = XML.getNodes(d, "//OriginalText");

                if(nodeList != null) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        nodeList.item(i).setTextContent("");
                    }
                }
            }

            if(!englishTextReqd) {

                NodeList nodeList = XML.getNodes(d, "//EnglishTranslatedText");

                if(nodeList != null) {

                    for (int i = 0; i < nodeList.getLength(); i++) {
                        nodeList.item(i).setTextContent("");
                    }
                }
            }

            if(!translatedTextReqd) {
                NodeList nodeList = XML.getNodes(d, "//TranslatedText");
                if(nodeList != null) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        nodeList.item(i).setTextContent("");
                    }
                }
            }


        } else {

            // return all text by default; nothing to do
        }

        log.info("build return kit:" + XML.parseToString(d));

    }

    // Add given field/value to existing XML document
    private static void addTo(Document existingDoc, String field, String value) throws Exception {
        Node targetNode = XML.getNode(existingDoc, "//" + field);
        if(targetNode != null) {
            targetNode.setTextContent(value);
        } else {
            // target node is null; future option is to create the missing field
//            Node SBCCommunicationDetailsNode = XML.getNode(existingDoc, "//SBCCommunicationDetails" );
//            Element newElement = existingDoc.createElement(field);
//            newElement.setTextContent(value);
//            SBCCommunicationDetailsNode.appendChild(newElement);
        }
    }

    // Override method with null field value if not provided
    private static void addTo(Document existingDoc, Element newDoc, String field) throws Exception {
        addTo(existingDoc, newDoc, field, null);
    }

    // Add given field/value to new XML document
    private static void addTo(Document existingDoc, Element newDoc, String field, String newValue) throws Exception {

        Node n = XML.getNode(existingDoc, "//" + field);
        if (n == null) n = existingDoc.createElement(field);
        if (newValue != null) {
            n.setTextContent(newValue);
        }
        n = newDoc.getOwnerDocument().adoptNode(n);
        newDoc.appendChild(n);

    }

    // Create new XML document object
    private static Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // action namespace and validating options on factory, if necessary
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();

    }
}
