package com.idiominc.ws.integration.compassion.servlet.TranslatorRatingUI;

import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSRole;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSWorkgroup;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Creates a class for processing the Translator Rating UI
 *
 *  @author SDL Professional Services
 */
public class TranslatorRatingCommands extends WSHttpServlet{

    private static final Logger log = Logger.getLogger(TranslatorRatingCommands.class);

    private static String RATING_ATTR = "rating";

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
            if ("getTranslators".equals(command)) {
                result = command_getTranslators(context, request);
            } else if ("getUserRating".equals(command)) {
                result = command_getUserRating(context, request);
            } else if ("getLocales".equals(command)) {
                result = command_getLocales(context);
            } else if ("addSourceLanguage".equals(command)) {
                command_addSourceLanguage(context, request);
            } else if ("addTargetLanguage".equals(command)) {
                command_addTargetLanguage(context, request);
            } else if ("deleteSourceLanguage".equals(command)) {
                command_deleteSourceLanguage(context, request);
            } else if ("deleteTargetLanguage".equals(command)) {
                command_deleteTargetLanguage(context, request);
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
     * @param request HTTP Request
     * @return Returns vendor list
     */
    public Object command_getTranslators(WSContext context, HttpServletRequest request)  {

        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();

        Boolean limitTranslators = Boolean.parseBoolean(request.getParameter("limitTranslators"));

        try {

            WSUser[] translators = context.getUserManager().getRole("Translators").getUsers();


            for (WSUser u : translators) {

                /** By default we include all translators */
                Boolean includeUser = true;

                /**
                 * If we are limiting then see if the current user and the current translator are in
                 * and of the same workgroups.
                 */
                if (limitTranslators) {
                    includeUser = workgroupIntersect(u.getWorkgroups(), context.getUser().getWorkgroups());
                }

                if (includeUser) {
                    // Create an key/value pair array to pass back to be used for populating a select box
                    Map<String, Object> userInfo = new HashMap<String, Object>();
                    userInfo.put("id", u.getId());
                    userInfo.put("name", u.getFullName());
                    ret.add(userInfo);
                }
            }
        } catch( Exception e ) {
            log.error("Could not get translators");
        }

        return ret;

    }


    /**
     * Returns a list of the locales in WorldServer
     *
     * @param context WS context
     * @return Mapped list of WS locales
     */
    public Object command_getLocales(WSContext context)  {

        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();

        try {

            // Get list of locales from WorldServer
            WSLocale[] locales = context.getUserManager().getLocales();

            // Loop through locales
            for (WSLocale locale : locales) {

                Map<String, Object> localeInfo = new HashMap<String, Object>();

                // Map id and name into associative array for easy import to <select> list
                localeInfo.put("id", locale.getId());
                localeInfo.put("name", locale.getName());

                // Add the new locale to the list to return
                ret.add(localeInfo);
            }
        } catch( Exception e ) {
            log.error("Cannot get Locales");
        }

        // Return sorted list of locales
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
    public Object command_getUserRating(WSContext context, HttpServletRequest request)  {

        // Get the userId from the HTTP request
        int userId = Integer.parseInt(request.getParameter("userId"));

        // Get the user from WorldServer
        WSUser user = context.getUserManager().getUser(userId);

        // Return the XML String rating attribute
        return user.getAttribute(RATING_ATTR);
    }


    /**
     * Adds a source language to the XML string storing the translator rating information
     *
     * @param context WS Context
     * @param request HTTP Request
     */
    public void command_addSourceLanguage(WSContext context, HttpServletRequest request) {

        // Get the userId and source language from the HTTP request
        int userId = Integer.parseInt(request.getParameter("userId"));
        String sourceLanguage = request.getParameter("sourceLanguage");

        // Either create or get and existing XML doc
        Document doc = getXMLDoc(context, userId);

        /** Check to see if the source language already exists */
        try {
            Node languagePair = XML.getNode(doc, "/language_pair[@source='" + sourceLanguage + "']");

            /** If it doesn't then add it, otherwise do nothing */
            if (languagePair == null) {

                // Add a new source language to the XML doc
                addTranslatorSourceLanguage(doc, sourceLanguage);

                // Write the XML information to the rating attribute on the user
                writeRatingAttr(context, doc, userId);

            }
        } catch (XPathException e) {
            log.error("Could not find XML data");
            e.printStackTrace();
        }

    }

    /**
     * Removes a source language, and all children, from the XML string storing translator rating.
     *
     * @param context WS Context
     * @param request HTTP Request
     */
    public void command_deleteSourceLanguage(WSContext context, HttpServletRequest request) {

        // Get the userId and source langauage from the HTTP request
        int userId = Integer.parseInt(request.getParameter("userId"));
        String sourceLanguage = request.getParameter("sourceLanguage");

        // Get the XML rating information for this user
        Document doc = getXMLDoc(context, userId);

        // Remove the source language from that rating info
        deleteTranslatorSourceLanguage(doc, sourceLanguage);

        // Write the XML data back to the WorldServer user rating attribute
        writeRatingAttr(context, doc, userId);

    }

    /**
     * Removes a target language from the XML string storing translator rating information
     *
     * @param context WS Context
     * @param request HTTP Request
     */
    public void command_deleteTargetLanguage(WSContext context, HttpServletRequest request) {

        // Get the userId, source and target languages from the HTTP request
        int userId = Integer.parseInt(request.getParameter("userId"));
        String sourceLanguage = request.getParameter("sourceLanguage");
        String targetLanguage = request.getParameter("targetLanguage");

        // Get the XML rating string from the WorldServer user
        Document doc = getXMLDoc(context, userId);

        // Remove the target language from the XML string
        deleteTranslatorTargetLanguage(doc, sourceLanguage, targetLanguage);

        // Write the modified XML back to the user
        writeRatingAttr(context, doc, userId);

    }


    /**
     * Adds a target language to the XML string storing translator rating
     *
     * @param context WS Context
     * @param request HTTP Request
     */
    public void command_addTargetLanguage(WSContext context, HttpServletRequest request) {

        // Get the userId, source and target languages, and rating from the HTTP request
        int userId = Integer.parseInt(request.getParameter("userId"));
        String sourceLanguage = request.getParameter("sourceLanguage");
        String targetLanguage = request.getParameter("targetLanguage");
        String languageRating = request.getParameter("rating");

        // Get the XML rating string from the WorldServer user
        Document doc = getXMLDoc(context, userId);

        // Add a new target language, or if already exists, update the rating
        addTranslatorTargetLanguage(doc,
                sourceLanguage,
                targetLanguage,
                languageRating);

        // Save the XML back to the user rating attribute
        writeRatingAttr(context, doc, userId);

    }


    /******************************************************************************************************************
     *
     *  XML Helper methods
     *
     ******************************************************************************************************************/


    /**
     * Gets the XML Rating information from either the user attribute if it exists, or creates a new
     * XML doc ready for rating information
     *
     * @param context WS Context
     * @param userId WorldServer userId value
     * @return XML Document
     */
    private static Document getXMLDoc(WSContext context, int userId) {

        // Get the user from WorldServer
        WSUser user = context.getUserManager().getUser(userId);

        // Get the XML rating string from the user
        String translatorRating = user.getAttribute(RATING_ATTR);

        Document doc = null;

        try {
            if (translatorRating != null) {

                /** If the user attribute exists then parse it */
                doc = XML.parseXML(translatorRating);

            } else {
                /** If the user attribute doesn't exist then create a new XML doc */
                doc = createTranslatorRatingXML();
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
     * Writes the XML translator rating string to a WorldServer user attribute
     *
     * @param context WS Context
     * @param doc The XML string containing translator rating information
     * @param userId WorldServer userId to receive the XML string
     */
    private static void writeRatingAttr(WSContext context, Document doc, int userId) {

        String xmlString = null;

        // Get the user from WorldServer
        WSUser user = context.getUserManager().getUser(userId);

        try {
            // Turn the XML doc into a string in order to save
            xmlString = XML.serializeNode(doc);
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        // Save the XML to the WorldServer user rating attribute
        user.setAttribute(RATING_ATTR, xmlString);
    }


    /**
     * Creates the DOM structure for the TranslatorRating document
     *
     * @return Returns the document
     * @throws ParserConfigurationException
     */
    private static Document createTranslatorRatingXML() throws ParserConfigurationException {


        // Create a new XML doc
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();

        // Create the root <translator_rating> tag
        Element rootElement = doc.createElement("translator_rating");

        // Add the root element to the XML doc
        doc.appendChild(rootElement);

        return doc;
    }


    /******************************************************************************************************************
     *
     *  WorldServer interface methods
     *
     ******************************************************************************************************************/

    /**
     * Creates a new language pair
     *
     * @param doc The XML Document
     * @param sourceLanguage The source language in a string that matches WS
     */
    private static void addTranslatorSourceLanguage( Document doc,
                                                     String sourceLanguage) {


        try {

            // Get the source language node from the XML doc
            Node existingSource = XML.getNode(doc,"//language_pair[@source='" + sourceLanguage + "']");

            /** If the source language does not already exist then create it */
            if (existingSource == null) {
                /** Get the top level <translator_rating> tag */
                NodeList translatorRating = doc.getElementsByTagName("translator_rating");
                Element rootElement =  (Element) translatorRating.item(0);

                /** Create a new language pair tag */
                Element languagePair = doc.createElement("language_pair");
                rootElement.appendChild(languagePair);

                /** Set the source language*/
                Attr attr = doc.createAttribute("source");
                attr.setValue(sourceLanguage);
                languagePair.setAttributeNode(attr);

                /** Since the source language should always be unique for each user we
                 * will use that for ID, this makes it much easier to grab this element
                 * in the TargetLanguage section
                 */

                languagePair.setIdAttribute("source", true);
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }



    }

    /**
     * Adds target languages to an existing language pair
     *
     * @param doc XML document contain rating information
     * @param sourceLanguage The language to translate from in WorldServer Locale name string
     * @param targetLanguage The language to translate to in WorldServer Locale name string
     * @param rating The string value of the rating
     */
    private static void addTranslatorTargetLanguage( Document doc,
                                                     String sourceLanguage,
                                                     String targetLanguage,
                                                     String rating) {

        /** Get the language pair node based on the source language, which is
         *  set at the ID
         */
        Element sourceNode;

        try {
            sourceNode = (Element) XML.getNode(doc, "//language_pair[@source='" + sourceLanguage + "']");

            /** Check to make sure that exists */
            if (sourceNode != null) {

                // Get the target node, if it exists, from the XML doc
                Element targetNode = (Element) XML.getNode(doc, "//language_pair[@source='" + sourceLanguage + "']/target[text()='" + targetLanguage + "']");

                Attr attr;

                // Check to see if the target node exists
                if (targetNode == null)
                {
                    /** Create the <target> tag*/
                    targetNode = doc.createElement("target");

                    /** Add the tag into the DOM */
                    sourceNode.appendChild(targetNode);

                    /** Create a rating attribute and set to the desired value*/
                    attr = doc.createAttribute("rating");

                    /** Set the value of the <target> tag to the target language*/
                    targetNode.setTextContent(targetLanguage);
                } else {

                    /** If the target language already exists get the existing attribute */
                    attr = targetNode.getAttributeNode("rating");
                }

                // Set the value of the rating attribute
                attr.setValue(rating);

                // Set the attribute on the target node
                targetNode.setAttributeNode(attr);

            } else {
                log.error("Source language " + sourceLanguage + "does not exist.");
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }
    }


    /**
     * Deletes target languages from an existing language pair
     *
     * @param doc XML Document containing rating information
     * @param sourceLanguage The WorldServer locale string name for the language to translate from
     */
    private static void deleteTranslatorSourceLanguage( Document doc,
                                                        String sourceLanguage ) {

        try {
            // Get the source language node from the XML
            Element sourceElement = (Element) XML.getNode(doc, "//language_pair[@source='" + sourceLanguage + "']");

            // Check to see if it exists
            if (sourceElement != null) {
                // Remove source language node
                sourceElement.getParentNode().removeChild(sourceElement);
            } else {
                log.warn("Source language " + sourceLanguage + " does not exist.");
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }
    }


    /**
     * Deletes target languages from an existing language pair
     *
     * @param doc XML document containing rating information
     * @param sourceLanguage The WorldServer locale string name for the language to translate from
     * @param targetLanguage The WorldServer locale string name for the language to translate to
     */
    private static void deleteTranslatorTargetLanguage( Document doc,
                                                        String sourceLanguage,
                                                        String targetLanguage ) {

        try {
            // Get the target language node
            Node targetNode = XML.getNode(doc,"//language_pair[@source='" + sourceLanguage + "']/target[text()='" + targetLanguage + "']");

            // Verify that it exists
            if (targetNode != null) {
                // Delete the target language node from the source language
                targetNode.getParentNode().removeChild(targetNode);
            } else {
                log.warn("Target node does not exist.");
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


    /**
     * Check to see if the user has a workgroup role of Translators
     *
     * @param user WS user
     * @return True if yes false if no
     */
    public static boolean isTranslator(WSUser user) {
        for(WSRole role: user.getRoles()) {
            if(role.getName().equals("Translators")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check to see if two lists of workgroups have any overlap
     *
     * @param user1Workgroups First array to compare
     * @param user2Workgroups Second array to compare
     * @return True or false depending on whether any intersection occurs
     */
    public static boolean workgroupIntersect(WSWorkgroup[] user1Workgroups, WSWorkgroup[] user2Workgroups) {

        Set<WSWorkgroup> s1 = new HashSet<WSWorkgroup>(Arrays.asList(user1Workgroups));
        Set<WSWorkgroup> s2 = new HashSet<WSWorkgroup>(Arrays.asList(user2Workgroups));
        s1.retainAll(s2);

        return s1.size() > 0;

    }

    /******************************************************************************************************************
     *
     *  WorldServer Configuration Methods
     *
     ******************************************************************************************************************/

    public String getName() {
        return "translator_ui_ajax_commands";
    }

    public String getDescription() {
        return "Translator UI AJAX commands";
    }

    public String getVersion() {
        return "1.0";
    }


    /******************************************************************************************************************
     *
     *  Test Methods
     *
     ******************************************************************************************************************/

    /**
     * Main() - Testing method
     *
     * @param args Standard Main args
     */
    public static void main(String[] args) {
        WSContextManager.runAsUser("admin", "wsadmin", new WSRunnable() {

            public boolean run(WSContext context) {

                for (WSUser user : context.getUserManager().getUsers()) {
                    if (isTranslator(user)) {
                        try {
                            Document doc = createTranslatorRatingXML();

                            addTranslatorSourceLanguage(doc, "en-US");
                            addTranslatorSourceLanguage(doc, "es-ES");
                            addTranslatorSourceLanguage(doc, "fr-FR");


                            addTranslatorTargetLanguage(doc, "en-US", "es-ES", "Expert");
                            addTranslatorTargetLanguage(doc, "en-US", "fr-FR", "Expert");
                            addTranslatorTargetLanguage(doc, "en-US", "de-DE", "Trainee");

                            addTranslatorTargetLanguage(doc, "es-ES", "en-US", "Expert");

                            addTranslatorTargetLanguage(doc, "fr-FR", "en-US", "Intermediate");


                            try {
                                Transformer transformer;

                                transformer = TransformerFactory.newInstance().newTransformer();
                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                                StreamResult result = new StreamResult(new StringWriter());
                                DOMSource source = new DOMSource(doc);
                                try {
                                    transformer.transform(source, result);
                                } catch (TransformerException e) {
                                    e.printStackTrace();
                                }
                                String xmlString = result.getWriter().toString();

                                user.setAttribute(RATING_ATTR, xmlString);

                            } catch (TransformerConfigurationException e) {
                                e.printStackTrace();
                            }

                        } catch (ParserConfigurationException e) {
                            e.printStackTrace();
                        }


                    }
                }

                return true;
            }
        });

    }




}
