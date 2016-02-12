package com.idiominc.ws.integration.compassion.utilities.rating;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSWorkgroup;

//dom and sax
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//java
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.util.List;
import java.io.StringReader;
import java.io.IOException;


/**
 * This abstract class is responsible for
 * parsing default complexity rule configuration data
 *
 * @author SDL Professional Services
 */

public abstract class WBAssignmentDefaultRuleIdentifier {

    //constants
    protected static final String _ASSIGNMENTRULE_ATTRIBUTE = "userAssignmentRule";

    //data - default document
    private Document default_document = null;

    /**
     * Constructor
     * @param context WS Context
     * @throws RatingException - IO or parsing exception
     */
    protected WBAssignmentDefaultRuleIdentifier(WSContext context) throws RatingException {
      try {
        String rule = getDefaultRule(context);
        default_document = build(rule);
      } catch (Exception e) {
          throw new RatingException(e.getLocalizedMessage());
      }
    }

    /**
     * Builds and return XML document from the rule string
     * @param rule - rule string
     * @return - document
     * @throws ParserConfigurationException - config exception
     * @throws IOException - IO exception
     * @throws SAXException - SAX exception
     */

    protected Document build(final String rule) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(rule)));
    }

    /**
     * This method obtains the list of valid ratings for given complexity and workgroup
     * @param applicability - QC or TRANSLATION
     * @param complexity - level of complexity
     * @return List of valid ratings per complexity/workgroup
     * @throws RatingException - thrown if parsing exception occurs
     */
    public abstract List<RATING> getListOfRatingsPerComplexity(final RATING_APPLICABILITY applicability,
                                                      final String complexity) throws RatingException;

    /**
     * Perform XPath operation
     * @param doc - Document to query against. If null, query the "default" document
     * @param query - query
     * @return - node that was satisfies the query
     * @throws XPathExpressionException - exception
     */
    protected Node performXPathQuery(Document doc,
                                     final String query) throws XPathExpressionException {
        if(null == doc) doc = default_document;
        if(null == doc) throw new XPathExpressionException("Document Node is null");
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile(query);
        return (Node)expr.evaluate(doc, XPathConstants.NODE);
    }


    /**
     * Obtains the contents translatorRating WS user attribute which contains an XML string with the rating information
     * for source->target language translation
     *
     * @param context WorldServer Context
     * @return Returns the XML String
     */
    private String getDefaultRule(WSContext context)  {

        // Get the default value from the attribute
        String defaultRule = context.getAttributeManager().getAttributeDescriptor(
                 WSWorkgroup.class,
                _ASSIGNMENTRULE_ATTRIBUTE).getDefaultValue();

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
            context.getAttributeManager().getAttributeDescriptor(WSWorkgroup.class,
                                                                 _ASSIGNMENTRULE_ATTRIBUTE).setDefaultValue(defaultRule);

        }

        // Return the default value
        return defaultRule;
    }

}
