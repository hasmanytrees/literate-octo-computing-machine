package com.idiominc.ws.integration.compassion.restService;

import org.json.simple.JSONObject;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by bslack on 9/23/15.
 */
public class OAuthToken {

    private String type;
    private String token;
    private int ttl;
    private Date expiredDate;

    public OAuthToken(String type, String token, int ttl) {
        this.type = type;
        this.token = token;
        this.ttl = ttl;

        Calendar expiredCal = Calendar.getInstance();
        expiredCal.setTime(new Date());
        expiredCal.add(Calendar.SECOND, ttl);

        this.expiredDate = expiredCal.getTime();

    }

    public String getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public int getTtl() {
        return ttl;
    }

    public boolean isExpired() {
        return new Date().after(expiredDate);
    }

    public String toString() {
        return "OAuthToken{" +
                "type='" + type + '\'' +
                ", token='" + token + '\'' +
                ", ttl=" + ttl +
                ", expiredDate=" + expiredDate +
                '}';
    }
}