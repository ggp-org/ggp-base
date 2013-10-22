package org.ggp.base.util.crypto;

import org.ggp.base.util.crypto.BaseCryptography.EncodedKeyPair;

import external.JSON.JSONException;
import external.JSON.JSONObject;
import junit.framework.TestCase;

/**
 * Unit tests for the SignableJSON class, which provides an easy way
 * for code to sign JSON objects using PK/SK pairs, and check whether
 * a particular object has been signed.
 * 
 * @author Sam
 */
public class Test_SignableJSON extends TestCase {
    public void testSimpleSigning() throws JSONException {
        EncodedKeyPair p = BaseCryptography.generateKeys();
        
        JSONObject x = new JSONObject("{3:{7:9,c:4,2:5,a:6},1:2,2:3,moves:14,states:21,alpha:'beta'}");
        assertFalse(SignableJSON.isSignedJSON(x));
        SignableJSON.signJSON(x, p.thePublicKey, p.thePrivateKey);
        assertTrue(SignableJSON.isSignedJSON(x));
        assertTrue(SignableJSON.verifySignedJSON(x));
        
        JSONObject x2 = new JSONObject(x.toString().replace(",", ", ").replace("{", "{ ").replace("}", "} "));
        assertTrue(SignableJSON.isSignedJSON(x2));
        assertTrue(SignableJSON.verifySignedJSON(x2));
        
        JSONObject x3 = new JSONObject("{1:2,2:3,3:4}");
        assertFalse(SignableJSON.isSignedJSON(x3));
    }
}