package org.ggp.base.util.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * HttpRequest is a helper class that encapsulates all of the code necessary
 * for a match host to issue a long-lived HTTP request to a player, wait for
 * the response, and return it. This is a key part of the GGP gaming protocol.
 *
 * @author schreib
 */
public final class HttpRequest
{
	public static String issueRequest(String targetHost, int targetPort, String forPlayerName, String requestContent, int timeoutClock) throws IOException {
		Socket socket = new Socket();
    	InetAddress theHost = InetAddress.getByName(targetHost);
    	socket.connect(new InetSocketAddress(theHost.getHostAddress(), targetPort), 5000);
    	HttpWriter.writeAsClient(socket, theHost.getHostName(), requestContent, forPlayerName);
    	String response = (timeoutClock < 0) ? HttpReader.readAsClient(socket) : HttpReader.readAsClient(socket, timeoutClock);
    	socket.close();
    	return response;
	}
}
