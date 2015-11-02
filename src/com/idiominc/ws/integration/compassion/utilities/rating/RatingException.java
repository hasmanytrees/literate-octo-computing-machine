package com.idiominc.ws.integration.compassion.utilities.rating;

/**
 * Special exception class for Rating operations and reassignments
 */
public class RatingException extends Exception {

    /**
     * Default Ctor
     */
    public RatingException() {
        super();
    }

    /**
     * Conversion ctor
     * @param message - error messsage
     */
    public RatingException(String message) {
        super (message);
    }

}
