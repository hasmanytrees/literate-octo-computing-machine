package com.idiominc.external;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic shared utility class
 *
 * @author SDL Professional Services
 */
public class Utils {


    /**
     * XML core namespace
     */
    public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    /**
     * XML namespace for xmlns attributes
     */
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

    /**
     * XML Schema Instance namespace
     */
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * XML Schema Instance namespace
     */
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    /**
     * XLink namespace
     */
    public static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    /**
     * Transformer factory.
     */
    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    }

    /**
     * Serializes a source into a writer.
     *
     * @param source to serialize.
     * @param writer writer to serialize into.
     * @throws javax.xml.transform.TransformerException
     *          If an unrecoverable error occurs during the
     *          course of the serialization.
     */
    public static void serialize(Source source, Writer writer)
            throws TransformerException {
        Transformer transformer;
        synchronized (TRANSFORMER_FACTORY) {
            transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty("standalone","yes");

        }


        transformer.transform(source, new StreamResult(writer));
    }

    /**
     * Serializes DOM node into a writer.
     *
     * @param node   DOM node to serialize.
     * @param writer writer to serialize into.
     * @throws javax.xml.transform.TransformerException
     *          If an unrecoverable error occurs during the
     *          course of the serialization.
     */
    public static void serialize(Node node, Writer writer)
            throws TransformerException {
        serialize(new DOMSource(node), writer);
    }

    /**
     * Parse document from a string source
     *
     * @param xml Source XML string
     * @return Document if parse succeded
     * @throws Exception on parse failure
     */
    public static Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // action namespace and validating options on factory, if necessary
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);

        return builder.parse(source);
    }

    public static String getText(Element root, String ns, String elName) throws Exception {
        Node el = getChild(root, ns, elName);
        Node child = el.getFirstChild();
        return child != null ? child.getNodeValue() : "";
    }

    public static Element getChild(Document root, String chNS, String elName)
            throws IOException {
        NodeList nl = root.getElementsByTagNameNS(chNS, elName);
        Element child = (Element) nl.item(0);
        if (child == null)
            throw new IOException("Element not found: " + elName);

        return child;
    }


    private static Element getChild(Element root, String chNS, String elName)
            throws IOException {
        NodeList nl = root.getElementsByTagNameNS(chNS, elName);
        Element child = (Element) nl.item(0);
        if (child == null)
            throw new IOException("Element not found: " + elName);

        return child;
    }

    public static String getRequestXML(Document doc) throws IOException {
        return getRequestXML(doc, true);

    }

    /**
     * Serialize XML document to string
     *
     * @param doc Document to serialize
     * @return String with XML contents
     * @throws Exception on serialization failure
     */
    public static String getRequestXML(Document doc, boolean pretty) throws IOException {
        OutputFormat format = new OutputFormat(doc);    //Serialize DOM
        format.setIndenting(pretty);
        StringWriter stringOut = new StringWriter();    //Writer will be a String
        XMLSerializer serial = new XMLSerializer(stringOut, format);
        serial.setNamespaces(true);
        serial.asDOMSerializer();                        // As a DOM Serializer

        serial.serialize(doc.getDocumentElement());
        return stringOut.toString();                    //Spit out DOM as a String
    }


    public static File getRequestXMLAsFile(Document doc, boolean pretty, File f) throws IOException {
        OutputFormat format = new OutputFormat(doc);    //Serialize DOM
        format.setIndenting(pretty);
        FileWriter fw = new FileWriter(f);    //Writer will be a String
        XMLSerializer serial = new XMLSerializer(fw, format);
        serial.setNamespaces(true);
        serial.asDOMSerializer();                        // As a DOM Serializer


        serial.serialize(doc.getDocumentElement());
        fw.close();

        return f;
    }


    /**
     * Parse document from a string source
     *
     * @param xml Source XML string
     * @return Document if parse succeded
     * @throws Exception on parse failure
     */
    public static Document parseResponse(String xml) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // action namespace and validating options on factory, if necessary
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);

        return builder.parse(source);

    }

    public static String serializeNode(Node node) throws TransformerException {
        StringWriter stringOut = new StringWriter();
        serialize(node, stringOut);
        return stringOut.toString();

    }

    public static String parseToString(Node node) throws TransformerException {
        return parseToString(node, true);
    }

    public static String parseToString(Node node, boolean stripHeader) throws TransformerException {

        StringBuffer buffer = new StringBuffer();

        //todo: force known Tridion namespaces into transformer, or this will only work with Xalan (which is
        //todo: is more forgiving than Xerces, but technically incorrect!
        TransformerFactory t = TransformerFactory.newInstance();
        Transformer transformer = t.newTransformer();
        if (stripHeader) transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter stringWriter = new StringWriter(128);

        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        buffer = stringWriter.getBuffer();

        return buffer.toString();
    }

    public static File parseToFile(Node node, boolean stripHeader, File f) throws TransformerException, IOException {


        //todo: force known Tridion namespaces into transformer, or this will only work with Xalan (which is
        //todo: is more forgiving than Xerces, but technically incorrect!
        TransformerFactory t = TransformerFactory.newInstance();
        Transformer transformer = t.newTransformer();
        if (stripHeader) transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        FileWriter fw = new FileWriter(f);

        transformer.transform(new DOMSource(node), new StreamResult(fw));
        fw.close();
        //buffer = stringWriter.getBuffer();

        //return buffer.toString();
        return f;
    }

    public static String[] simpleXPathNodeContentResults(Object obj, String xpath) throws XPathExpressionException {

        //XPath runPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = runPath.compile(xpath);
        NodeList nl = (NodeList) expr.evaluate(obj, XPathConstants.NODESET);

        String[] ret = new String[nl.getLength()];
        for (int x = 0; x < ret.length; x++) {
            //todo: fix this exception and  parsing handling!
            try {
                ret[x] = parseToString(nl.item(x));
            } catch (Exception e) {
                throw new XPathExpressionException(e);
            }
        }

        return ret;
    }

    public static String[] getFields(Object obj, String xpath) throws XPathExpressionException {
        //XPath runPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = runPath.compile(xpath);
        NodeList nl = (NodeList) expr.evaluate(obj, XPathConstants.NODESET);

        String[] ret = new String[nl.getLength()];
        for (int x = 0; x < ret.length; x++) {
            ret[x] = nl.item(x).getTextContent();
        }

        return ret;
    }

    private static final Map<String, XPathExpression> cache = new HashMap<String, XPathExpression>();

    public static String getField(Object obj, String xpath) throws XPathException {
        XPathExpression toExec = null;
        if (!cache.containsKey(xpath)) {
            toExec = runPath.compile(xpath);
            cache.put(xpath, toExec);
        } else {
            toExec = cache.get(xpath);
        }


        //System.out.println("runPath.evaluate(xpath,obj)=" + runPath.evaluate(xpath,obj));
        String ret = toExec.evaluate(obj);
        if ("".equals(ret))
            return null;

        return ret;

    }


    private static final XPath runPath = XPathFactory.newInstance().newXPath();

    public static Node getNode(Object obj, String xpath) throws XPathException {

        XPathExpression toExec = null;
        if (!cache.containsKey(xpath)) {
            toExec = runPath.compile(xpath);
            cache.put(xpath, toExec);
        } else {
            toExec = cache.get(xpath);
        }


        Node n = (Node) toExec.evaluate(obj, XPathConstants.NODE);


        return n;

    }


    public static NodeList getNodes(Object obj, String xpath) throws XPathExpressionException {

        XPathExpression toExec = null;
        if (!cache.containsKey(xpath)) {
            toExec = runPath.compile(xpath);
            cache.put(xpath, toExec);
        } else {
            toExec = cache.get(xpath);
        }

        NodeList nl = (NodeList) toExec.evaluate(obj, XPathConstants.NODESET);


        if (nl.getLength() > 0)
            return nl;

        return null;
    }

    public static String getFieldContent(Object obj, String xpath) throws XPathExpressionException {
        //XPath runPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = runPath.compile(xpath);
        Node n = (Node) expr.evaluate(obj, XPathConstants.NODE);
        if (n == null)
            return null;
        try {
            return parseToString(n);
        } catch (Exception e) {
            throw new RuntimeException(e); //todo: FIX
        }

    }

    public static String[] getFieldContents(Object obj, String xpath) throws XPathExpressionException {

        //XPath runPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = runPath.compile(xpath);
        NodeList nl = (NodeList) expr.evaluate(obj, XPathConstants.NODESET);

        String[] ret = new String[nl.getLength()];
        for (int x = 0; x < ret.length; x++) {
            try {
                ret[x] = parseToString(nl.item(x));
            } catch (Exception e) {
                throw new RuntimeException(e); //todo: FIX
            }
        }

        return ret;
    }

    public static String getFieldContentsString(Object obj, String xpath) throws XPathExpressionException {

        String[] allFields = getFieldContents(obj, xpath);
        StringBuffer buf = new StringBuffer(1024 * 128);
        for (int x = 0; x < allFields.length; x++) {
            buf.append(allFields[x]).append("\n");
        }

        return buf.toString();
    }

    public static Document load(File f) throws ParserConfigurationException, SAXException, IOException {
        FileInputStream fis = new FileInputStream(f);
        Document d = load(new InputSource(fis));
        fis.close();
        return d;

    }

    public static Document load(InputSource source) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        return docBuilder.parse(source);
    }




    public static File getStringAsFile(File tmpFile, String content) throws IOException {


        Reader in = new StringReader(content);
        Writer out = new FileWriter(tmpFile);
        copyReader(in, out);

        in.close();
        out.close();

        return tmpFile;

    }

    public static File getStringAsFile(File tmpFile, String content, String encoding) throws IOException {


        Reader in = new StringReader(content);
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(tmpFile), encoding);
        copyReader(in, out);

        in.close();
        out.close();

        return tmpFile;

    }


    public static void copyReader(Reader in, Writer out) throws IOException {

        // Transfer bytes from in to out
        char[] buf = new char[1024 * 4];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }


        out.flush();
    }

    public static String loadFileAsString(File file) throws IOException {
        Reader in = new InputStreamReader(new FileInputStream(file));
        String retStr = getReaderAsString(in);
        in.close();
        return retStr;
    }

    public static String getReaderAsString(Reader reader) throws IOException {

        StringWriter out = new StringWriter();
        copyReader(reader, out);
        String retString = out.getBuffer().toString();
        out.close();
        return retString;

    }
}
