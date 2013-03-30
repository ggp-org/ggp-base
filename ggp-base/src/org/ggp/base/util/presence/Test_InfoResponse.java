package org.ggp.base.util.presence;

import junit.framework.TestCase;

/**
 * Unit tests for the BaseCryptography class, which implements
 * a wrapper for the use of asymmetric public/private key cryptography
 * for use in GGP.
 * 
 * @author Sam
 */
public class Test_InfoResponse extends TestCase {
    public void testFormingInfoResponse() {
    	InfoResponse response = new InfoResponse();
    	assertEquals(response.toSymbol().toString(), "( )");
    	response.setName("PlayerName");
    	assertEquals(response.toSymbol().toString(), "( ( name PlayerName ) )");
    	response.setStatus("available");
    	assertEquals(response.toSymbol().toString(), "( ( name PlayerName ) ( status available ) )");
    }
    
    public void testParsingInfoResponse() {
    	String input = "( ( name PlayerName ) ( status available ) )";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), "PlayerName");
    	assertEquals(response.getStatus(), "available");
    }
    
    public void testParsingInfoResponseWithExtras() {
    	String input = "( whatsup ( name PlayerName ) ( ( foo bar ) baz ) ( status available ) zzq )";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), "PlayerName");
    	assertEquals(response.getStatus(), "available");
    }

    public void testParsingInfoResponseWithNoStatus() {
    	String input = "( whatsup ( ) ( baz ) ( name PlayerName ) )";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), "PlayerName");
    	assertEquals(response.getStatus(), null);
    }
    
    public void testParsingInfoResponseWithNoInfo() {
    	String input = "( )";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), null);
    	assertEquals(response.getStatus(), null);
    }
    
    public void testParsingBadlyFormedInfoResponse() {
    	String input = "(";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), null);
    	assertEquals(response.getStatus(), null);
    }
    
    public void testParsingStatusOnlyInfoResponse() {
    	String input = "busy";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), null);
    	assertEquals(response.getStatus(), "busy");
    }
    
    public void testParsingInfoResponseLegacyJSON() {
    	String input = "{\"name\":\"PlayerName\",\"status\":\"available\"}";
    	InfoResponse response = InfoResponse.create(input);
    	assertEquals(response.getName(), "PlayerName");
    	assertEquals(response.getStatus(), "available");
    }
}