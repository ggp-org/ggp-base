package org.ggp.base.util.presence;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the BaseCryptography class, which implements
 * a wrapper for the use of asymmetric public/private key cryptography
 * for use in GGP.
 *
 * @author Sam
 */
public class InfoResponseTest extends Assert {
    @Test
    public void testFormingInfoResponse() {
        InfoResponse response = new InfoResponse();
        assertEquals(response.toSymbol().toString(), "( )");
        response.setName("PlayerName");
        assertEquals(response.toSymbol().toString(), "( ( name PlayerName ) )");
        response.setStatus("available");
        assertEquals(response.toSymbol().toString(), "( ( name PlayerName ) ( status available ) )");
    }

    @Test
    public void testParsingInfoResponse() {
        String input = "( ( name PlayerName ) ( status available ) )";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), "PlayerName");
        assertEquals(response.getStatus(), "available");
    }

    @Test
    public void testParsingInfoResponseWithExtras() {
        String input = "( whatsup ( name PlayerName ) ( ( foo bar ) baz ) ( status available ) zzq )";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), "PlayerName");
        assertEquals(response.getStatus(), "available");
    }

    @Test
    public void testParsingInfoResponseWithNoStatus() {
        String input = "( whatsup ( ) ( baz ) ( name PlayerName ) )";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), "PlayerName");
        assertEquals(response.getStatus(), null);
    }

    @Test
    public void testParsingInfoResponseWithNoInfo() {
        String input = "( )";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), null);
        assertEquals(response.getStatus(), null);
    }

    @Test
    public void testParsingBadlyFormedInfoResponse() {
        String input = "(";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), null);
        assertEquals(response.getStatus(), null);
    }

    @Test
    public void testParsingStatusOnlyInfoResponse() {
        String input = "busy";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), null);
        assertEquals(response.getStatus(), "busy");
    }

    @Test
    public void testParsingInfoResponseLegacyJSON() {
        String input = "{\"name\":\"PlayerName\",\"status\":\"available\"}";
        InfoResponse response = InfoResponse.create(input);
        assertEquals(response.getName(), null);
        assertEquals(response.getStatus(), input);
    }
}