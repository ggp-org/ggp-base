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
        // TODO: It should be safe to use "readContentFromPOST(br)" rather than having a special
        // function that just reads a single line, but some servers won't send back content-length,
        // and a server (player) should never have a multi-line response.
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		return readSingleLineFromResponse(br);
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
		} else if (requestLine.toUpperCase().startsWith("POST ")) {
		    message = readContentFromPOST(br);
		} else if (requestLine.toUpperCase().startsWith("OPTIONS ")) {
		    // Web browsers can send an OPTIONS request in advance of sending
		    // real XHR requests, to discover whether they should have permission
		    // to send those XHR requests. We want to handle this at the network
		    // layer rather than sending it up to the actual player, so we write
		    // a blank response (which will include the headers that the browser
		    // is interested in) and throw an exception so the player ignores the
		    // rest of this request.
		    HttpWriter.writeAsServer(socket, "");
		    throw new IOException("Drop this message at the network layer.");
		} else {
		    HttpWriter.writeAsServer(socket, "");
            throw new IOException("Unexpected request type: " + requestLine);		    
		}
		
		return message;
	}
	
	private static String readContentFromPOST(BufferedReader br) throws IOException {
	    String line;
        int theContentLength = -1;
        StringBuilder theContent = new StringBuilder();        
        while ((line = br.readLine()) != null) {
            if (line.toLowerCase().startsWith("content-length:")) {
                theContentLength = Integer.parseInt(line.toLowerCase().replace("content-length:", "").trim());
            } else if (line.length() == 0) {
              // We want to ignore the headers in the request, so we'll just
              // ignore every line up until the first blank line. The content
              // of the request appears after that. We do pull in the header
              // that indicates the content-length, so we know how much content
              // to read in, once we reach the content.
              if (theContentLength != -1) {
                  for (int i = 0; i < theContentLength; i++) {
                      theContent.append((char)br.read());
                  }
                  return theContent.toString().trim();
              } else {
                  throw new IOException("Could not find Content-Length header in POST request.");
              }
            }
        }
        throw new IOException("Could not find content in POST request.");
	}
	
    // Private helper methods that handle common HTTP tasks.        
    private static String readSingleLineFromResponse(BufferedReader br) throws IOException {        
        boolean reachedContent = false;
        String line;
        while ((line = br.readLine()) != null){
            if (reachedContent) {
                return line;
            }
            if (line.length() == 0) {
                // We want to ignore the headers in the request, so we'll just
                // ignore every line up until the first blank line. The content
                // of the request appears after that.
                reachedContent = true;
            }
        }
        throw new IOException("Could not find content in response.");
    }
}