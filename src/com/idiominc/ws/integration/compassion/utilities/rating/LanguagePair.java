package com.idiominc.ws.integration.compassion.utilities.rating;

//sdk
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.WSContext;

//Java
import java.util.Map;
import java.util.HashMap;

/**
 * The "protected" class to store rating per language pair for any user
 *
 * @author SDL Professional Services
 */
public class LanguagePair {

    private String sourceLocaleName;
    private Map<String, RATING> ratings = new HashMap<String,RATING>();

    /**
     * Contructor
     * @param sourceLocaleName - source locale name as defined in WorldServer
     */
    protected LanguagePair(String sourceLocaleName) {
        this.sourceLocaleName = sourceLocaleName;
    }

    /**
     * Add rating to the list of ratings user can operate on
     * @param targetLocaleName - target locale name
     * @param rating - string representation of rating
     */
    protected void addRating(String targetLocaleName,
                          String rating) {
        RATING rt = RATING.fromValue(rating);
        if(null != rt) {
           ratings.put(targetLocaleName, rt);
        }
    }

    /**
     * Check if user can work on this source locale's project
     * @param context - ws context
     * @param sourceLocale - WorldServer's project source locale
     * @return TRUE if user can take on this project based on the source locale
     */
    protected boolean isQualifiedPair(final WSContext context,
                                   final WSLocale sourceLocale)  {
        return null != sourceLocale
               &&
                sourceLocale.getName().equals(sourceLocaleName);
    }

    /**
     * Obtain rating for the target locale
     * @param targetLocale - WS object that represents project's target locale
     * @return User rating for this source-target locale pair
     * @throws RatingException - user is not qualified
     */
    protected RATING getRating(final WSLocale targetLocale) throws RatingException {
        String targetLocaleName = targetLocale.getName();
        if(ratings.containsKey(targetLocaleName)) {
            return ratings.get(targetLocaleName);
        }
         throw new RatingException("No rating is available for locale " +
                                   targetLocale.getName());
    }

}
