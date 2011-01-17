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
		return readMessageContent(br);
	}

	public static String readAsServer(Socket socket) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		// The first line of the HTTP request is the request line.
		String requestLine = br.readLine();
		String message;
		if(requestLine.toUpperCase().startsWith("GET ")) {
		    message = requestLine.substring(5, requestLine.lastIndexOf(' '));
		    message = URLDecoder.decode(message, "UTF-8");
		    message = message.replace((char)13, ' ');
		} else {
		    message = readMessageContent(br);
		}		
		
		return message;
	}	
	
	// Private helper methods that handle common HTTP tasks.	
	private static String readMessageContent(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean reachedContent = false;
        String line;
        while ((line = br.readLine()) != null){
            if (reachedContent) {
                sb.append(line + "\n");
            }            
            if (line.length() == 0) {
                reachedContent = true;
            }
        }
        return sb.toString().trim();	    
	}
}