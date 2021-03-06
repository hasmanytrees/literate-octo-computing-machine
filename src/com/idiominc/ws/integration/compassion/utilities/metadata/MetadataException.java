package com.idiominc.ws.integration.compassion.utilities.metadata;

/**
 * Special exception class for Attribute assignment
 *
 * @author SDL Professional Services
 */

public class MetadataException extends Exception {

    /**
     * Default Ctor
     */
    public MetadataException() {
        super();
    }

    /**
     * Conversion ctor
     * @param message - error messsage
     */
    public MetadataException(String message) {
        super (message);
    }
}
