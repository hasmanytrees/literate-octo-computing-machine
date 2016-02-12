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

//java
import javax.xml.xpath.*;
import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;

/**
 * This class is responsible for
 * parsing workgroup-based complexity rule configuration data
 *
 * @author SDL Professional Services
 */

public class WBAssignmentRuleIdentifier extends WBAssignmentDefaultRuleIdentifier {

    //constants
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
      //call parent's ctor
        super(context);
      try {
        AttributeValidator.validateAttribute(context,
                                             _ASSIGNMENTRULE_ATTRIBUTE,
                                             ATTRIBUTE_OBJECT.WORKGROUP,
                                             ATTRIBUTE_TYPE.TEXT,
                                             workgroup,
                                             "");
        String rule = workgroup.getAttributeValue(_ASSIGNMENTRULE_ATTRIBUTE).getAttributeValue();
        document = build(rule);
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
              Node ratingsNode = performXPathQuery(document, query);
              if(null == ratingsNode || null == ratingsNode.getTextContent() || 0 == ratingsNode.getTextContent().length()) {
                 ratingsNode = performXPathQuery(null, query);
                 if(null == ratingsNode || null == ratingsNode.getTextContent() || 0 == ratingsNode.getTextContent().length()) {
                   throw new RatingException("Assignment rules per workgroup and complexity are not available, " +
                                             "and so are default rules");
                 }
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

}