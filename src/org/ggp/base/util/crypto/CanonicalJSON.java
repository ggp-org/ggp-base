package org.ggp.base.util.crypto;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;
import external.JSON.JSONString;

public class CanonicalJSON {
    /* Right now we only support one canonicalization strategy, which is
     * the SIMPLE approach. In the future, we may need to make breaking changes
     * to the canonicalization strategy, to support unforeseen situations (e.g.
     * edge cases the current canonicalization strategy doesn't handle properly).
     * However, we'd still like to be able to canonicalize data using the older
     * strategies so that we can e.g. still verify signatures created using the
     * older canonicalization strategy. So this class is designed to be able to
     * support multiple canonicalization strategies, and the user chooses which
     * strategy is used. */
    static enum CanonicalizationStrategy {
        SIMPLE,
    };

    /* Helper function to generate canonical strings for JSON strings */
    static String getCanonicalForm(String x, CanonicalizationStrategy s) {
        try {
            return getCanonicalForm(new JSONObject(x), s);
        } catch (JSONException e) {
            return null;
        }        
    }
    
    /* Main function to generate canonical strings for JSON objects */
    static String getCanonicalForm(JSONObject x, CanonicalizationStrategy s) {
        if (s == CanonicalizationStrategy.SIMPLE) {
            return renderSimpleCanonicalJSON(x);
        } else {
            throw new RuntimeException("Canonicalization strategy not recognized.");
        }
    }
    
    /* This should be identical to the standard code to render the JSON object,
     * except it forces the keys for maps to be listed in sorted order. */
    static String renderSimpleCanonicalJSON(Object x) {
        try {
            if (x instanceof JSONObject) {
                JSONObject theObject = (JSONObject)x;
                
                // Sort the keys
                TreeSet<String> t = new TreeSet<String>();
                Iterator<?> i = theObject.keys();
                while (i.hasNext()) t.add(i.next().toString());
                Iterator<String> keys = t.iterator();
                
                StringBuffer sb = new StringBuffer("{");    
                while (keys.hasNext()) {
                    if (sb.length() > 1) {
                        sb.append(',');
                    }
                    Object o = keys.next();
                    sb.append(JSONObject.quote(o.toString()));
                    sb.append(':');
                    sb.append(renderSimpleCanonicalJSON(theObject.get(o.toString())));
                }
                sb.append('}');
                return sb.toString();
            } else if (x instanceof JSONArray) {
                JSONArray theArray = (JSONArray)x;
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                int len = theArray.length();
                for (int i = 0; i < len; i += 1) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(renderSimpleCanonicalJSON(theArray.get(i)));
                }
                sb.append("]");
                return sb.toString();
            } else {
                if (x == null || x.equals(null)) {
                    return "null";
                }
                if (x instanceof JSONString) {
                    Object object;
                    try {
                        object = ((JSONString)x).toJSONString();
                    } catch (Exception e) {
                        throw new JSONException(e);
                    }
                    if (object instanceof String) {
                        return (String)object;
                    }
                    throw new JSONException("Bad value from toJSONString: " + object);
                }
                if (x instanceof Number) {
                    return JSONObject.numberToString((Number)x);
                }
                if (x instanceof Boolean || x instanceof JSONObject ||
                        x instanceof JSONArray) {
                    return x.toString();
                }
                if (x instanceof Map) {
                    return renderSimpleCanonicalJSON(new JSONObject((Map<?,?>)x)).toString();
                }
                if (x instanceof Collection) {
                    return renderSimpleCanonicalJSON(new JSONArray((Collection<?>)x)).toString();
                }
                if (x.getClass().isArray()) {
                    return renderSimpleCanonicalJSON(new JSONArray(x)).toString();
                }
                return JSONObject.quote(x.toString());
            }
        } catch (Exception e) {
            return null;
        }            
    }
}