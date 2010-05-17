package util.http;

import java.net.URLDecoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

public final class HttpReader
{
    // Wrapper methods to support socket timeouts for reading requests/responses.
    
    public static String readAsClient(Socket socket, int timeout) throws IOException, SocketTimeoutException
    {
        socket.setSoTimeout(timeout);
        return readAsClient(socket);
    }
    
    public static String readAsServer(Socket socket, int timeout) throws IOException, SocketTimeoutException
    {
        socket.setSoTimeout(timeout);
        return readAsServer(socket);
    }
    
    // Implementations of reading HTTP responses (readAsClient) and
    // HTTP requests (readAsServer) for the purpose of communicating
    // with other general game playing systems.
    
	public static String readAsClient(Socket socket) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		// The first line of the HTTP response is the status line.
		// For now, we ignore it, assuming that it is always OK.
		br.readLine();
		
		String message = readHeadersAndMessage(br);

		return message;
	}

	public static String readAsServer(Socket socket) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		// The first line of the HTTP request is the request line.
		String requestLine = br.readLine().toUpperCase();
		String message;
		if(requestLine.startsWith("GET ")) {
		    message = requestLine.substring(5, requestLine.lastIndexOf(' '));
		    message = URLDecoder.decode(message, "UTF-8");
		    readHeadersAndMessage(br);
		} else {
		    message = readHeadersAndMessage(br);
		}		
		
		return message;
	}	
	
	// Private helper methods that handle common HTTP tasks.

	private static String readHeadersAndMessage(BufferedReader br) throws IOException {
	    // We are reading a HTTP request, as per the HTTP 1.1 spec (RFC 2616).        
	    // This method assumes that we have read the first line, which is the
	    // request line in a request, or the response line in a response.
	    //
        // Subsequent lines, up until the first blank line, are headers.
        // We are currently interested only in the "Content-length" header,
        // which indicates the length of the message body. 
        String line;
        int length = 0;
        do {
            line = br.readLine();
            if(line.toLowerCase().startsWith("content-length: ")) {
                length = Integer.valueOf(line.substring(16));
            }
        } while(line.length() > 0);   
        
        if(length == 0)
            return "";

        // Finally, we have the message body. 
        char rawData[] = new char[length];
        for (int i = 0; i < length; i++)
        {
            rawData[i] = (char) br.read();
        }

        return new String(rawData);	    
	}
}