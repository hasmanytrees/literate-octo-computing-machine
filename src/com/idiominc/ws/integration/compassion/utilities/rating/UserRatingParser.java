package com.idiominc.ws.integration.compassion.utilities.rating;

//internal dependencies
import com.idiominc.external.config.Config;

//SAX
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

//java
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.MalformedURLException;

//sdk
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.WSAttributeNotSupportedException;
import com.idiominc.wssdk.WSContext;


/**
 * The class to parse individual User's rating per language pairs
 */
public class UserRatingParser {

    //data
    private List<LanguagePair> ratingsList = null;
    private String userName;

    /**
     * Constructor
     * @param user - WS User Object
     * @throws RatingException - parsing exception
     */
    public UserRatingParser(final WSUser user) throws RatingException {
        XMLReader xmlReader;
        try {
            if(null == user) {
                throw new RatingException("User object is not defined");
            }
            userName = user.getUserName();
            xmlReader = XMLReaderFactory.createXMLReader();
            UserRatingHandler handler = new UserRatingHandler();
            xmlReader.setContentHandler(handler);
            String ratingData = user.getAttributeValue(Config._USER_RATING_ATTRIBUTE).getAttributeValue();
            if(null == ratingData || 0 == ratingData.length()) {
                throw new RatingException("Rating Data is not available for user " + userName);
            }
            xmlReader.parse(new InputSource(
                    new ByteArrayInputStream(ratingData.getBytes("utf-8"))));
            ratingsList = handler.getLanguagePairs();
        } catch (SAXException e) {
            throw new RatingException(e.getLocalizedMessage());
        } catch(WSAttributeNotSupportedException e) {
            throw new RatingException(e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            throw new RatingException(e.getLocalizedMessage());
        } catch (IOException e) {
            throw new RatingException(e.getLocalizedMessage());
        }
    }

    /**
     * Obtain user rating (if found) for language pair
     * @param context - WorldServer Context
     * @param sourceLocale - source locale object
     * @param targetLocale - target locale object
     * @return User Rating
     * @throws RatingException - rating not found
     */
    public RATING getRating(WSContext context,
                            WSLocale sourceLocale,
                            WSLocale targetLocale) throws RatingException {
        if(null == ratingsList) {
           throw new RatingException("No valid rating data is defined for user "
                                     + userName);
        }
        for(LanguagePair lp: ratingsList) {
            if(lp.isQualifiedPair(context, sourceLocale)) {
                return lp.getRating(targetLocale);
            }
        }
        throw new RatingException("User " + userName + " is not qualified to work " +
                                  sourceLocale.getLanguage().getName() + " - " +
                                  targetLocale.getLanguage().getName() + " translation "
                                  );
    }

  private class UserRatingHandler extends DefaultHandler {

    //List to hold language pairs
    private List<LanguagePair> lpList = null;

    //temporary data objects
    private LanguagePair lp = null;
    private String rating = null;

    //getter method for collected list
    public List<LanguagePair> getLanguagePairs() {
        return lpList;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("language_pair")) {
            //create a new language pair and put it in list
            String sourceLocaleName = attributes.getValue("source");
            lp = new LanguagePair(sourceLocaleName);
            //initialize list
            if (lpList == null) {
                lpList = new ArrayList<LanguagePair>();
            }
            rating = null;
        } else if (qName.equalsIgnoreCase("target")) {
            rating = attributes.getValue("rating");
        } else {
            rating = null;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("language_pair")) {
            //add language pair object to the list
            if(lp != null && lpList != null) {
              lpList.add(lp);
            }
        } else if(qName.equalsIgnoreCase("target")) {
            rating = null;
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if (rating != null && lp != null) {
            lp.addRating(new String(ch, start, length), rating);
        }
    }

  }

}