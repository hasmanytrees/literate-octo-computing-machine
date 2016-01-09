package com.idiominc.ws.integration.compassion.servlet.UserAssignmentRuleUI;

import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import com.idiominc.wssdk.user.WSWorkgroup;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class process XML data associated with the User Assignment Rule GUI
 *
 * @author SDL Professional Services
 */
public class UserAssignmentRuleCommands extends WSHttpServlet {


    private static final Logger log = Logger.getLogger(UserAssignmentRuleCommands.class);

    public synchronized boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        try {

            // Get the command from the HTTP request
            String command = request.getParameter("command");

            // Send a valid HTTP response
            response.setContentType("application/json");
            response.setStatus(200);

            Object result = null;

            /**
             * Parse commands from the HTTP request, assign them to the corresponding method
             */
            if ("getWorkgroups".equals(command)) {
                result = command_getWorkgroups(context);
            } else if ("getAssignmentRule".equals(command)) {
                result = command_getAssignmentRule(context, request);
            } else if ("setAssignmentRule".equals(command)) {
                command_setAssignmentRule(context, request);
            } else if ("deleteAssignmentRule".equals(command)) {
                command_deleteAssignmentRule(context, request);
            } else if ("getDefaultRule".equals(command)) {
                result = command_getDefaultRule(context);
            }

            if (result instanceof File) {
                File file = (File) result;
                if ("yes".equals(request.getParameter("download"))) {
                    asDownload(response, file);
                } else {
                    JSONObject.writeJSONString(asMetadata(request, file), response.getWriter());
                }
            } else {
                JSONValue.writeJSONString(result, response.getWriter());
            }

            // Always commit so always return true
            return true;

        } catch (Exception e) {

            log.error("API ERROR", e);

            response.setStatus(400);

            try {
                JSONValue.writeJSONString((e.getMessage() != null ? e.getMessage() : "UNKNOWN"), response.getWriter());
            } catch (Exception e2) {
                log.error("API ERROR", e);
            }

            return false;
        }
    }


    /******************************************************************************************************************
     *
     *  command_ methods
     *
     ******************************************************************************************************************/

    /**
     * Returns a list of translators in alphabetical order in order to populate the select box
     *
     * @param context WS context
     * @return Returns vendor list
     */
    public Object command_getWorkgroups(WSContext context)  {

        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();

        try {

            // Get all workgroups belonging to the current user
            WSWorkgroup[] workgroups = context.getUser().getWorkgroups();

            // Loop throgh workgroups and build a map for using in select box
            for (WSWorkgroup workgroup: workgroups) {

                Map<String, Object> workgroupInfo = new HashMap<String, Object>();
                workgroupInfo.put("id", workgroup.getId());
                workgroupInfo.put("name", workgroup.getName());
                ret.add(workgroupInfo);
            }

        } catch( Exception e ) {
            log.error("Could not get workgroups.");
        }

        return ret;
    }


    /**
     * Obtains the contents translatorRating WS user attribute which contains an XML string with the rating information
     * for source->target language translation
     *
     * @param context WorldServer Context
     * @param request HTTP Request
     * @return Returns the XML String
     */
    public Object command_getAssignmentRule(WSContext context, HttpServletRequest request)  {

        // Get the workgroup from the HTTP request
        int workgroupID = Integer.parseInt(request.getParameter("workgroupId"));

        // Get the requested workgroup
        WSWorkgroup workgroup = context.getUserManager().getWorkgroup(workgroupID);

        // Get the attribute containing the XML from the workgroup and return it
        return workgroup.getAttribute("userAssignmentRule");
    }


    /**
     * Obtains the contents translatorRating WS user attribute which contains an XML string with the rating information
     * for source->target language translation
     *
     * @param context WorldServer Context
     * @return Returns the XML String
     */
    public Object command_getDefaultRule(WSContext context)  {

        // Get the default value from the attribute
        String defaultRule = context.getAttributeManager().getAttributeDescriptor( WSWorkgroup.class,
                "userAssignmentRule").getDefaultValue();

        // Check to see if it exists
        if (defaultRule == null) {

            // This is a default for the default - If no default value is set for the custom attribute use this one
            // as a structure and ultimate default

            defaultRule = "<assignment_rule>\n" +
                    "  <rule type=\"Translate\">\n" +
                    "    <complexity level=\"1\">\n" +
                    "       Trainee,Beginner,Intermediate,Advanced,Expert\n" +
                    "    </complexity>\n" +
                    "    <complexity level=\"2\">\n" +
                    "       Intermediate,Advanced,Expert\n" +
                    "    </complexity>\n" +
                    "    <complexity level=\"3\">\n" +
                    "       Advanced,Expert\n" +
                    "    </complexity>\n" +
                    "  </rule>\n" +
                    "  <rule type=\"QC\">\n" +
                    "    <complexity level=\"1\">\n" +
                    "       Advanced,Expert\n" +
                    "    </complexity>\n" +
                    "    <complexity level=\"2\">\n" +
                    "       Advanced,Expert\n" +
                    "    </complexity>\n" +
                    "    <complexity level=\"3\">\n" +
                    "       Expert\n" +
                    "    </complexity>\n" +
                    "  </rule>\n" +
                    "</assignment_rule>";

            // If the default doesn't exist then set it to the above
            context.getAttributeManager().getAttributeDescriptor( WSWorkgroup.class,
                    "userAssignmentRule").setDefaultValue(defaultRule);

            log.warn("Default value for userAssignmentRule workgroup attribute did not exist, set to backup default.");
        }

        // Return the default value
        return defaultRule;
    }


    /**
     * Adds a source language to the XML string storing the translator rating information
     *
     * @param context WS Context
     * @param request HTTP Request
     */
    public void command_setAssignmentRule(WSContext context, HttpServletRequest request) {

        // Get values from the HTTP request
        int workgroupID = Integer.parseInt(request.getParameter("workgroupID"));
        String ruleType = request.getParameter("ruleType");
        int complexityLevel = Integer.parseInt(request.getParameter("complexityLevel"));
        String ruleValue = request.getParameter("ruleValue");

        // Either create or get and existing XML doc
        Document doc = getXMLDoc(context, workgroupID);

        // Add or modify a complexity rule in the XML doc
        addComplexityRule(doc, ruleType, complexityLevel, ruleValue);

        // Write the XML information to the rating attribute on the user
        writeUserAssignmentRuleAttr(context, doc, workgroupID);

    }


    /**
     * Removes a target language from the XML string storing translator rating information
     *
     * @param context WS Context
     * @param request HTTP Request
     */
    public void command_deleteAssignmentRule(WSContext context, HttpServletRequest request) {

        // Get values from the HTTP request
        int workgroupID = Integer.parseInt(request.getParameter("workgroupID"));
        String ruleType = request.getParameter("ruleType");
        int complexityLevel = Integer.parseInt(request.getParameter("complexityLevel"));

        // Get the XML rating string from the WorldServer workgroup
        Document doc = getXMLDoc(context, workgroupID);

        // Remove the complexity rule from the XML string
        deleteComplexityRule(doc, ruleType, complexityLevel);

        // Write the modified XML back to the user
        writeUserAssignmentRuleAttr(context, doc, workgroupID);

    }


    /******************************************************************************************************************
     *
     *  XML Helper methods
     *
     ******************************************************************************************************************/


    /**
     * Gets the XML Rating information from either the workgroup attribute if it exists, or creates a new
     * XML doc ready for rule information
     *
     * @param context WS Context
     * @param workgroupID WorldServer userId value
     * @return XML Document
     */
    private static Document getXMLDoc(WSContext context, int workgroupID) {

        // Get the workgroup from WorldServer
        WSWorkgroup workgroup = context.getUserManager().getWorkgroup(workgroupID);

        // Get the XML rating string from the workgroup
        String userAssignmentRuleXML = workgroup.getAttribute("userAssignmentRule");

        Document doc = null;

        try {
            if (userAssignmentRuleXML != null) {

                /** If the workgroup attribute exists then parse it */
                doc = XML.parseXML(userAssignmentRuleXML);

            } else {
                /** If the workgroup attribute doesn't exist then create a new XML doc */
                doc = createUserAssignmentRuleXML();
            }
        } catch (ParserConfigurationException e) {
            log.error("Could not parse XML.");
            e.printStackTrace();
        } catch (Exception e) {
            log.error("Could not create new XML doc");
            e.printStackTrace();
        }

        return doc;
    }


    /**
     * Write a new, or update existing, rule
     *
     * @param context - WorldServer context
     * @param doc - XML doc
     * @param workgroupID - ID of workgroup
     */

    private static void writeUserAssignmentRuleAttr(WSContext context, Document doc, int workgroupID) {

        String xmlString = null;

        // Get the workgroup from WorldServer
        WSWorkgroup workgroup = context.getUserManager().getWorkgroup(workgroupID);
        try {
            // Turn the XML doc into a string in order to save
            xmlString = XML.serializeNode(doc);
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        // Save the XML to the WorldServer workgroup rating attribute
        workgroup.setAttribute("userAssignmentRule", xmlString);
    }


    /**
     * Creates the DOM structure for the TranslatorRating document
     *
     * @return Returns the document
     * @throws ParserConfigurationException
     */
    private static Document createUserAssignmentRuleXML() throws ParserConfigurationException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("assignment_rule");
        rootElement.appendChild(doc.createElement("translate_rule"));
        rootElement.appendChild(doc.createElement("QC_rule"));
        doc.appendChild(rootElement);

        return doc;
    }


    /******************************************************************************************************************
     *
     *  WorldServer interface methods
     *
     ******************************************************************************************************************/

    /**
     * Adds a complexity rule to the XML data stored in the Workgroup attribute
     *
     * @param doc - The XML data
     * @param ruleType - The type of rule "translate_rule" or "QC_rule"
     * @param complexityLevelValue - Int value representing complexity
     * @param ruleValue - The data to store in this rule
     */

    private static void addComplexityRule( Document doc,
                                           String ruleType,
                                           int complexityLevelValue,
                                           String ruleValue ) {

        try {

            // Get the base rule node
            Element ruleNode = (Element) XML.getNode(doc, "/rule[@type='" + ruleType + "']");

            // Get the specific complexity level node
            Element complexityLevel = (Element) XML.getNode(doc, "//rule[@type='" + ruleType + "']/complexity[@level='" + complexityLevelValue + "']");

            Attr attr;

            if( complexityLevel == null ) {

                /** Create the <complexity> tag*/
                complexityLevel = doc.createElement("complexity");

                /** Add the tag into the DOM */
                ruleNode.appendChild(complexityLevel);

                /** Create a level attribute and set to the desired value*/
                attr = doc.createAttribute("level");

            } else {
                /** If the target language already exists get the existing attribute */
                attr = complexityLevel.getAttributeNode("level");
            }

            // Set the value of the attribute
            attr.setValue(String.valueOf(complexityLevelValue));
            complexityLevel.setAttributeNode(attr);

            /** Set the value of the <complexity> tag to the target ratings*/
            complexityLevel.setTextContent(ruleValue);

        } catch (XPathException e) {
            e.printStackTrace();
        }
    }


    /**
     * Remove the complexity rule from a Workgroup XML attribute
     *
     * @param doc - The XML document data
     * @param ruleType - The rule type "translate_rule" or "QC_rule"
     * @param complexityLevelValue - An integer value of the complexity level
     */

    private static void deleteComplexityRule( Document doc,
                                              String ruleType,
                                              int complexityLevelValue ) {

        try {

            // Get the specific complexity level value
            Element complexityLevel = (Element) XML.getNode(doc, "//" + ruleType + "/complexity[@level="
                    + complexityLevelValue + "]");

            // If that worked...
            if (complexityLevel != null) {

                // Remove the element from the DOM
                complexityLevel.getParentNode().removeChild(complexityLevel);
            } else {
                log.warn("Complexity level " + complexityLevelValue + " in rule " + ruleType + " does not exist.");
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }
    }


    /******************************************************************************************************************
     *
     *  General helper methods
     *
     ******************************************************************************************************************/

    /**
     * Retrieve HTTP as download file
     *
     * @param response HTTP response
     * @param file File to download
     * @throws IOException
     */

    private void asDownload(HttpServletResponse response, File file) throws IOException {
        response.setContentType("application/octet");
        String fn = file.getName().substring(file.getName().indexOf("_") + 1);
        if (fn.startsWith("upload_")) fn = fn.substring(fn.indexOf(".") + 1);
        response.setHeader("Content-Disposition", "file;filename=\"" + fn + "\"");//URLEncoder.encode(file.getName().substring(file.getName().indexOf("_") + 1)));
        FileUtils.fileToStream(file, response.getOutputStream());
    }


    /**
     * Get HTTP as metadata
     *
     * @param request HTTP request
     * @param file File to receive
     * @return Return metadata
     */
    private Map<String, String> asMetadata(HttpServletRequest request, File file) {
        return asMetadata(request, file, null);
    }


    /**
     * Get HTTP as metadata
     *
     * @param request HTTP Request
     * @param file File to receive
     * @param command Command to process
     * @return Mapped value
     */
    private Map<String, String> asMetadata(HttpServletRequest request, File file, String command) {
        String queryString = request.getQueryString();
        if (command != null) {
            queryString = queryString.replaceFirst("(.*command=)(.*?)(&)", "$1" + command + "$3");
        }
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("name", file.getName().substring(file.getName().indexOf("_") + 1));
        ret.put("link", request.getContextPath() + "/" + request.getServletPath() + "?" + queryString + "&download=yes");
        return ret;
    }


    /******************************************************************************************************************
     *
     *  WorldServer Configuration Methods
     *
     ******************************************************************************************************************/

    public String getName() {
        return "user_assignment_ajax_commands";
    }

    public String getDescription() {
        return "User Assignment UI AJAX commands";
    }

    public String getVersion() {
        return "1.0";
    }

}
