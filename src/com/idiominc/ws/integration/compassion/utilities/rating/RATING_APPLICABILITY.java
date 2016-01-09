package com.idiominc.ws.integration.compassion.utilities.rating;

/**
 * Rating can be applied to TRANSLATE and QA steps
 *
 * @author SDL Professional Services
 */
public enum RATING_APPLICABILITY {
    TRANSLATE ("Translate"),
    QC ("QC");

    //rating text
    private final String text;

    /**
     * Ctor
     * @param txt - text
     */
    RATING_APPLICABILITY(String txt) {
        this.text = txt;
    }

    /**
     * to String
     * @return text
     */
    public String toString() {
        return this.text;
    }

}
