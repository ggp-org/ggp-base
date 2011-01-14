package apps.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;

// TODO(schreib): Remove this file... just checking it in so that I can pick it up
// on another machine when I'm on vacation.
public class TestTool {
    // Private helper methods that handle common HTTP tasks.
    private static String readMessageContent(Socket s) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));        
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null){
            sb.append(line + "\n");
        }
        return sb.toString();
    }
    
    public static void writeClientRequest(Socket socket, InetAddress theAddress) throws IOException
    {
        PrintWriter socketOut = new PrintWriter(socket.getOutputStream());
        socketOut.print("GET / HTTP/1.0\n");
        socketOut.print("Host: " + theAddress.getHostName() + "\n");
        socketOut.print("Accept: */* \n\n");
        socketOut.flush();        
    }         
    
    public static void main(String[] args) {
        Socket s;
        try {
            InetAddress theAddress = InetAddress.getByName("ggp-webkiosk.appspot.com");
            System.out.println(theAddress.getHostName());
            System.out.println(theAddress.getHostAddress());
            s = new Socket(theAddress.getHostAddress(), 80);
            writeClientRequest(s, theAddress);                
            String theMessage = readMessageContent(s);
            System.out.println("-------------------");
            System.out.println(theMessage);
            System.out.println("-------------------");
            System.out.println(String.valueOf(MessageDigest.getInstance("MD5").digest(theMessage.getBytes())));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}