package util.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

public final class HttpReader
{

	public static String readAsClient(Socket socket) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		br.readLine();
		br.readLine();
		int length = readLength(br);
		br.readLine();

		char rawData[] = new char[length];
		for (int i = 0; i < length; i++)
		{
			rawData[i] = (char) br.read();
		}

		return new String(rawData);
	}

	public static String readAsClient(Socket socket, int timeout) throws IOException, SocketTimeoutException
	{
		socket.setSoTimeout(timeout);
		return readAsClient(socket);
	}

	public static String readAsServer(Socket socket) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		br.readLine();
		br.readLine();
		br.readLine();
		br.readLine();
		br.readLine();
		int length = readLength(br);
		br.readLine();

		char rawData[] = new char[length];
		for (int i = 0; i < length; i++)
		{
			rawData[i] = (char) br.read();
		}

		return new String(rawData);
	}

	public static String readAsServer(Socket socket, int timeout) throws IOException, SocketTimeoutException
	{
		socket.setSoTimeout(timeout);
		return readAsServer(socket);
	}

	private static int readLength(BufferedReader br) throws IOException
	{
		try
		{
			return Integer.valueOf(br.readLine().substring(16));
		}
		catch (Exception e)
		{
			throw new IOException("Unable to parse length!");
		}
	}

}