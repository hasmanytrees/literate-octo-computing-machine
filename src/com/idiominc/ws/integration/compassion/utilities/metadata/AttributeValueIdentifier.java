package com.idiominc.ws.integration.compassion.utilities.metadata;

//dom
import org.w3c.dom.Document;
import org.w3c.dom.Node;

//sax
import org.xml.sax.SAXException;

//java
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

/**
 * Parse source asset to identify attributes and their values
 *
 * @author SDL Professional Services
 */
public class AttributeValueIdentifier {

    /**
     * initialize document
     * @param asset - file handler to the source asset
     * @return - DOM DOCUMENT object
     * @throws MetadataException - IO exception
     */
    public static Document init(final File asset) throws MetadataException {
      try {
        return initializeDocument(asset);
      } catch (Exception ex) {
          throw  new MetadataException (ex.getLocalizedMessage());
      }
    }

    /**
     * Execute query and obtain value of the tag
     * @param doc - document
     * @param query - xpath query
     * @return - value of the tag
     * @throws MetadataException - xpath / query exception
     */
    public static String getValue(final Document doc,
                                  final String query)
                         throws MetadataException {
        try {
          if(doc == null) {
             throw new MetadataException("Payload File has not been parsed!");
          }
          Node result = (Node)performXPathQuery(doc, query, XPathConstants.NODE);
          if(result == null) {
             return null;
          } else {
            if(null == result.getTextContent()
               ||
               0 == result.getTextContent().length())
            {
                  return null;
            }
            return result.getTextContent();
          }
        } catch (XPathExpressionException ex) {
            throw new MetadataException(ex.getLocalizedMessage());
        }
    }


    /**
     * Perform XPath Query
     * @param doc - document
     * @param query - query string
     * @param qName - qName
     * @return - Node (if found), or null
     * @throws XPathExpressionException - query exception
     */
    public static Object performXPathQuery(final Document doc,
                                            final String query,
                                            final QName qName) throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile(query);
        if(qName.equals(XPathConstants.NODE)) {
           return expr.evaluate(doc, XPathConstants.NODE);
        } else {
           return expr.evaluate(doc, XPathConstants.NODESET);
        }
    }

    // Initialize DOM-based Document model based on input XML file
    /**
     * Helper to initialize the document
     * @param f - file
     * @return DOM document
     * @throws ParserConfigurationException - exception
     * @throws IOException - exception
     * @throws SAXException - exception
     */
    private static Document initializeDocument(final File f) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        return builder.parse(f);
    }

}