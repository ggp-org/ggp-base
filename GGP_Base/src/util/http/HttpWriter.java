package util.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public final class HttpWriter
{
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
		pw.println();
		pw.println(data);

		pw.flush();
	}

}
