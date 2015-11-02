package com.idiominc.ws.integration.compassion.utilities.rating;

//compassion
import com.idiominc.ws.integration.profserv.commons.CollectionUtils;
import com.idiominc.ws.integration.compassion.utilities.metadata.AttributeValidator;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_OBJECT;
import com.idiominc.ws.integration.compassion.utilities.metadata.ATTRIBUTE_TYPE;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSWorkgroup;

//dom and sax
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

//java
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;
import java.io.StringReader;

/**
 * This class is responsible for
 * parsing eorkgroup-based complexity rule configuration data
 */

public class WBAssignmentRuleIdentifier {

    //constants
    private static final String _ASSIGNMENTRULE_ATTRIBUTE = "userAssignmentRule";
    private static final String _XPATH = "//assignment_rule/rule[@type='%1$s']/complexity[@level='%2$s']";

    //data
    private Document document = null;

    /**
     * Provate constructor
     * @param context WS Context
     * @param workgroup - WorldServer workgroup
     * @throws RatingException - IO or parsing exception
     */
    public WBAssignmentRuleIdentifier(WSContext context,
                                      WSWorkgroup workgroup) throws RatingException {
      try {
        AttributeValidator.validateAttribute(context,
                                             _ASSIGNMENTRULE_ATTRIBUTE,
                                             ATTRIBUTE_OBJECT.WORKGROUP,
                                             ATTRIBUTE_TYPE.TEXT,
                                             workgroup,
                                             "");
        String rule = workgroup.getAttributeValue(_ASSIGNMENTRULE_ATTRIBUTE).getAttributeValue();
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        document = builder.parse(new InputSource(new StringReader(rule)));
      } catch (Exception e) {
          throw new RatingException(e.getLocalizedMessage());
      }
    }

    /**
     * This method obtains the list of valid ratings for given complexity and workgroup
     * @param applicability - QC or TRANSLATION
     * @param complexity - level of complexity
     * @return List of valid ratings per complexity/workgroup
     * @throws RatingException - thrown if parsing exception occurs
     */
    public List<RATING> getListOfRatingsPerComplexity(final RATING_APPLICABILITY applicability,
                                                      final String complexity) throws RatingException {
        try {
              String query = new Formatter(new StringBuffer()).format(_XPATH,
                                                                      applicability,
                                                                      complexity).toString();
              Node ratingsNode = performXPathQuery(query);
              if(null == ratingsNode) {
                 throw new RatingException("Assignment rules per workgroup and complexity are not available");
              }
              String ratingsNamesPerComplexity = ratingsNode.getTextContent();
              List<RATING> result = new ArrayList<RATING>();
              for(Object rating: CollectionUtils.csvToList(ratingsNamesPerComplexity)) {
                  if(rating instanceof String) {
                      RATING r = RATING.fromValue((String) rating);
                      if(r != null) {
                          result.add(r);
                      }
                  }
              }
              return result;
        } catch (XPathExpressionException ex) {
            throw new RatingException(ex.getLocalizedMessage());
        }
    }

    /**
     * Perform XPath operation
     * @param query - query
     * @return - node that was satisfies the query
     * @throws XPathExpressionException - exception
     */
    private Node performXPathQuery(final String query) throws XPathExpressionException {
        if(null == document) throw new XPathExpressionException("Document Node is null");
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile(query);
        return (Node)expr.evaluate(document, XPathConstants.NODE);
    }

}
