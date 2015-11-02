package com.idiominc.ws.integration.compassion.utilities.rating;

public enum RATING {

    TRAINEE("Trainee"),
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED ("Advanced"),
    EXPERT ("Expert");
    
    //text
    private final String text;

    /**
     * ctor
     * @param txt - text
     */
    RATING(String txt) {
        this.text = txt;
    }

    /**
     * to String
     * @return text
     */
    public String toString() {
        return this.text;
    }

    /**
     * Converts text to Rating
     * @param value - text
     * @return - RATING object
     */
    protected static RATING fromValue(String value) {
        for(RATING r: RATING.values()) {
            if(r.toString().equals(value)) {
                return r;
            }
        }
        return null;
    }

}
