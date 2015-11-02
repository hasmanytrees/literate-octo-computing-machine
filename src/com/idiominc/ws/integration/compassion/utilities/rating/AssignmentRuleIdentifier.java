package com.idiominc.ws.integration.compassion.utilities.rating;

//compassion
import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.profserv.commons.CollectionUtils;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.ais.WSNode;

//dom
import org.w3c.dom.Document;
import org.w3c.dom.Node;

//java
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;

/**
 * This class is responsible for
 * parsing global complexity rule configuration file
 * @depricated
 */
public class AssignmentRuleIdentifier {

    //static data
    private static AssignmentRuleIdentifier _instance = null;
    private static final String _XPATH_TRANSLATE = "//assignment_rule[@workgroup='%1$s']/translate_rule/complexity[@level='%2$s']";
    private static final String _XPATH_QC = "//assignment_rule[@workgroup='%1$s']/QC_rule/complexity[@level='%2$s']";
    private static final String _DEFAULT = "default";

    //data
    private Document document = null;

    /* singleton implementation */
    /**
     * Obtain the instance of the class
     * @param context - ws context
     * @return intance of the class
     * @throws RatingException - IO or parsing exception
     */
    public synchronized static AssignmentRuleIdentifier getInstance(WSContext context)
                               throws RatingException {
        if(_instance==null){
            _instance = new AssignmentRuleIdentifier(context);
        }
        return _instance;
    }

    /**
     * Provate constructor
     * @param context - WorldServer context
     * @throws RatingException - IO or parsing exception
     */
    private AssignmentRuleIdentifier(WSContext context) throws RatingException {
      try {
        WSNode node = context.getAisManager().getNode(Config.getAssignmentRulesConfigurationFile(context));
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        document = builder.parse(node.getFile());
      } catch (Exception e) {
          throw new RatingException(e.getLocalizedMessage());
      }
    }

    /**
     * This method obtains the list of valid ratings for given complexity and workgroup
     * @param applicability - QC or TRANSLATION
     * @param workgroup - project's workgroup
     * @param complexity - level of complexity
     * @return List of valid ratings per complexity/workgroup
     * @throws RatingException - thrown if parsing exception occurs
     */
    public List<RATING> getListOfRatingsPerComplexity(final RATING_APPLICABILITY applicability,
                               final WSWorkgroup workgroup,
                               final String complexity) throws RatingException {
        try {
              String query = new Formatter(new StringBuffer()).format((applicability == RATING_APPLICABILITY.TRANSLATE)?
                                                                      _XPATH_TRANSLATE : _XPATH_QC,
                                                                      workgroup.getName(),
                                                                      complexity).toString();
              Node ratingsNode = performXPathQuery(query);
              if(ratingsNode == null) {
                 query = new Formatter(new StringBuffer()).format((applicability == RATING_APPLICABILITY.TRANSLATE)?
                                                                  _XPATH_TRANSLATE : _XPATH_QC,
                                                                  _DEFAULT,
                                                                  complexity).toString();
                 ratingsNode = performXPathQuery(query);
              }
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
