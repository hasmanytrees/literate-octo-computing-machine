package com.idiominc.ws.integration.compassion.restService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Key/Value framework clsss, used by ESBHelper class to maintain and generate XML payload files.
 *
 * @author SDL Professional Services
 */
public class KV {

    /***
     * KV Key/Value pair. Uniqueness is determined by the KEY NAME only, not the value.
     */

    public static KV[] EMPTY = new KV[0];

    private String k, v;

    public KV(String k, String v) {
        this.k = k;
        this.v = v;
    }

    public KV(String k, int v) {
        this.k = k;
        this.v = v + "";
    }

    public String key() {
        return k;
    }

    public String value() {
        return v;
    }

    public static KV[] parse(String[] keys, String[] values) throws IOException {

        if (keys.length != values.length) {
            throw new IOException("Keys and values are invalid. They are different lengths. ");
        }

        List<KV> ret = new ArrayList<KV>();
        for (int x = 0; x < keys.length; x++) {
            ret.add(new KV(keys[x], values[x]));
        }

        return ret.toArray(new KV[ret.size()]);
    }

    public boolean equals(Object o) {
        // only use the key for equals/hashcode to prevent duplicates
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KV kv = (KV) o;

        if (k != null ? !k.equals(kv.k) : kv.k != null) return false;

        return true;
    }

    public int hashCode() {
        return k != null ? k.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "KV{" +
                "k='" + k + '\'' +
                ", v='" + v + '\'' +
                '}';
    }
}
