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

        pw.print("GET /" + URLEncoder.encode(data, "UTF-8") + " HTTP/1.0\r\n");
        pw.print("Accept: text/delim\r\n");
        pw.print("Host: " + hostField + "\r\n");
        pw.print("Sender: GAMESERVER\r\n");
        pw.print("Receiver: "+playerName+"\r\n");
        pw.print("\r\n");
        pw.print("\r\n");

        pw.flush();
    }

	public static void writeAsClient(Socket socket, String hostField, String data, String playerName) throws IOException
	{
		PrintWriter pw = new PrintWriter(socket.getOutputStream());

		pw.print("POST / HTTP/1.0\r\n");
		pw.print("Accept: text/delim\r\n");
		pw.print("Host: " + hostField + "\r\n");
		pw.print("Sender: GAMESERVER\r\n");
		pw.print("Receiver: "+playerName + "\r\n");
		pw.print("Content-Type: text/acl\r\n");
		pw.print("Content-Length: " + data.length() + "\r\n");
		pw.print("\r\n");
		pw.print(data);

		pw.flush();
	}

	public static void writeAsServer(Socket socket, String data) throws IOException
	{
		PrintWriter pw = new PrintWriter(socket.getOutputStream());

		pw.print("HTTP/1.0 200 OK\r\n");
		pw.print("Content-type: text/acl\r\n");
		pw.print("Content-length: " + data.length() + "\r\n");
		pw.print("Access-Control-Allow-Origin: *\r\n");
		pw.print("Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n");
		pw.print("Access-Control-Allow-Headers: Content-Type\r\n");
		pw.print("Access-Control-Allow-Age: 86400\r\n");
		pw.print("\r\n");
		pw.print(data);

		pw.flush();
	}

}
