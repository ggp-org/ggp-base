package util.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;

public final class HttpWriter
{
    public static void writeAsClientGET(Socket socket, String data, String playerName) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        PrintWriter pw = new PrintWriter(bw);

        pw.println("GET /" + URLEncoder.encode(data, "UTF-8") + " HTTP/1.0");
        pw.println("Accept: text/delim");
        pw.println("Sender: GAMESERVER");
        pw.println("Receiver: "+playerName);

        pw.flush();
    }
    
	public static void writeAsClient(Socket socket, String data, String playerName) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		PrintWriter pw = new PrintWriter(bw);

		pw.println("POST / HTTP/1.0");
		pw.println("Accept: text/delim");
		pw.println("Sender: GAMESERVER");
		pw.println("Receiver: "+playerName);
		pw.println("Content-type: text/acl");
		pw.println("Content-length: " + data.length());
		pw.println();
		pw.println(data);

		pw.flush();
	}

	public static void writeAsServer(Socket socket, String data) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		PrintWriter pw = new PrintWriter(bw);

		pw.println("HTTP/1.0 200 OK");
		pw.println("Content-type: text/acl");
		pw.println("Content-length: " + data.length());
		pw.println("Access-Control-Allow-Origin: *");
		pw.println("Access-Control-Allow-Methods: POST, GET, OPTIONS");
		pw.println("Access-Control-Allow-Headers: *");
		pw.println("Access-Control-Allow-Age: 86400");
		pw.println();
		pw.println(data);

		pw.flush();
	}

}
