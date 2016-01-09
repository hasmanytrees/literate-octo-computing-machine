package com.idiominc.ws.integration.compassion.restService;

/**
 * Generic internal REST exception
 *
 * @author SDL Professional Services
 */
public class RESTException extends Exception {

    private int httpCode;
    private long responseLength;
    private String responseText;

    public RESTException(String message, int httpCode, String responseText, long responseLength) {
        super(message);

        this.httpCode = httpCode;
        this.responseLength = responseLength;
        this.responseText = responseText;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public long getResponseLength() {
        return responseLength;
    }

    public String getResponseText() {
        return responseText;
    }

}
