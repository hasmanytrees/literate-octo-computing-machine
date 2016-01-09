package com.idiominc.ws.integration.compassion.utilities.twostepproject;

/**
 * Exception to be thrown should two-step
 * project creation fails
 *
 * @author SDL Professional Services
 */
public class TwoStepProjectException extends Exception {
    /**
     * Default ctor
     */
    public TwoStepProjectException() {
      super();
    }

    /**
     * Conversion constructor
     * @param msg - exceptions' message
     */
    public TwoStepProjectException (String msg) {
         super(msg);
    }

}