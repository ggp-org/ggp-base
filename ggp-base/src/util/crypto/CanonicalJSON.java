package util.crypto;

import external.JSON.JSONException;
import external.JSON.JSONObject;

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
            return x.toString();
        } else {
            throw new RuntimeException("Canonicalization strategy not recognized.");
        }
    }
}