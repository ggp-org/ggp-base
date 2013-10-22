package org.ggp.base.util.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;

public final class HttpWriter
{
    public static void writeAsClientGET(Socket socket, String hostField, String data, String playerName) throws IOException
    {
        PrintWriter pw = new PrintWriter(socket.getOutputStream());
        
        pw.println("GET /" + URLEncoder.encode(data, "UTF-8") + " HTTP/1.0");
        pw.println("Accept: text/delim");
        pw.println("Host: " + hostField);
        pw.println("Sender: GAMESERVER");
        pw.println("Receiver: "+playerName);
        pw.println();
        pw.println();
        
        pw.flush();
    }
    
	public static void writeAsClient(Socket socket, String hostField, String data, String playerName) throws IOException
	{
		PrintWriter pw = new PrintWriter(socket.getOutputStream());

		pw.println("POST / HTTP/1.0");
		pw.println("Accept: text/delim");
		pw.println("Host: " + hostField);
		pw.println("Sender: GAMESERVER");
		pw.println("Receiver: "+playerName);
		pw.println("Content-Type: text/acl");
		pw.println("Content-Length: " + data.length());
		pw.println();
		pw.print(data);
		
		pw.flush();
	}

	public static void writeAsServer(Socket socket, String data) throws IOException
	{
		PrintWriter pw = new PrintWriter(socket.getOutputStream());

		pw.println("HTTP/1.0 200 OK");
		pw.println("Content-type: text/acl");
		pw.println("Content-length: " + data.length());
		pw.println("Access-Control-Allow-Origin: *");
		pw.println("Access-Control-Allow-Methods: POST, GET, OPTIONS");
		pw.println("Access-Control-Allow-Headers: Content-Type");
		pw.println("Access-Control-Allow-Age: 86400");
		pw.println();
		pw.print(data);

		pw.flush();
	}

}
