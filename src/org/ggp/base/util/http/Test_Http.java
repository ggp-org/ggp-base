package org.ggp.base.util.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;

import junit.framework.TestCase;

/**
 * Unit tests for the HttpReader/HttpWriter pair, which are the way that
 * game players and game servers communicate. Please update these tests
 * as needed when bugs are discovered, to prevent regressions. 
 * 
 * @author Sam
 */
public class Test_Http extends TestCase {
    public void testSimpleEcho() throws IOException {
        SocketPair testPair = new SocketPair();
        doSimpleEchoCheck(testPair, "Hello World", "SamplePlayer");
    }
    
    public void testPathologicalEchos() throws IOException {
        SocketPair testPair = new SocketPair();
        doSimpleEchoCheck(testPair, "", "");
        doSimpleEchoCheck(testPair, "", "SamplePlayer");        
        doSimpleEchoCheck(testPair, "123 456 ^&!*! // 2198725 !@#$%^&*() DATA", "SamplePlayer");
        doSimpleEchoCheck(testPair, "abcdefgijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", "SamplePlayer");
        doSimpleEchoCheck(testPair, "Test String", "");
        doSimpleEchoCheck(testPair, "Test String", "!@#$%^&*()1234567890");
        doSimpleEchoCheck(testPair, "Test String", "abcdefgijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");                
    }
    
    public void testGenericPOSTs() throws IOException {
        SocketPair testPair = new SocketPair();
        doClientEchoCheckOverPOST(testPair, "", "");
        doClientEchoCheckOverPOST(testPair, "Test String", "");
        doClientEchoCheckOverPOST(testPair, "Test String", "Accept: text/delim\nSender: GAMESERVER");
        doClientEchoCheckOverPOST(testPair, "1234567890abcdefgijklmnopqrstuvwxyz!@#$%^&*()1234567890", "");
    }
    
    public void testGenericGETs() throws IOException {
        SocketPair testPair = new SocketPair();
        doClientEchoCheckOverGET(testPair, "", "");
        doClientEchoCheckOverGET(testPair, "Test String", "");
        doClientEchoCheckOverGET(testPair, "Test String", "Accept: text/delim\nSender: GAMESERVER");
        doClientEchoCheckOverGET(testPair, "1234567890abcdefgijklmnopqrstuvwxyz!@#$%^&*()1234567890", "");
    }
    
    // Helper functions for running specific checks.
    
    private void doSimpleEchoCheck(SocketPair p, String data, String playerName) throws IOException {
        HttpWriter.writeAsClient(p.client, "", data, playerName);
        String readData = HttpReader.readAsServer(p.server);
        assertEquals(readData.toUpperCase(), data.toUpperCase());
        
        HttpWriter.writeAsServer(p.server, data);
        readData = HttpReader.readAsClient(p.client);
        assertEquals(readData.toUpperCase(), data.toUpperCase());
    }
    
    private void doClientEchoCheckOverPOST(SocketPair p, String data, String headers) throws IOException {
        writeClientPostHTTP(p.client, headers, data);
        String readData = HttpReader.readAsServer(p.server);
        assertEquals(readData.toUpperCase(), data.toUpperCase());
    }
    
    private void doClientEchoCheckOverGET(SocketPair p, String data, String headers) throws IOException {
        writeClientGetHTTP(p.client, headers, data);
        String readData = HttpReader.readAsServer(p.server);
        assertEquals(readData.toUpperCase(), data.toUpperCase());
    }    
    
    // Helper functions for testing different types of HTTP interactions.
    
    private void writeClientPostHTTP(Socket writeOutTo, String headers, String data) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(writeOutTo.getOutputStream()));
        PrintWriter pw = new PrintWriter(bw);
    
        pw.println("POST / HTTP/1.0");
        if(headers.length() > 0) pw.println(headers);
        pw.println("Content-length: " + data.length());
        pw.println();
        pw.println(data);
        pw.flush();
    }
    
    private void writeClientGetHTTP(Socket writeOutTo, String headers, String data) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(writeOutTo.getOutputStream()));
        PrintWriter pw = new PrintWriter(bw);
    
        pw.println("GET /" + URLEncoder.encode(data, "UTF-8") + " HTTP/1.0");
        if(headers.length() > 0) pw.println(headers);
        pw.println("Content-length: 0");
        pw.println();
        pw.println();
        pw.flush();
    }    
    
    // Utility class to create a pair of client/server sockets
    // on the local machine that are connected to each other.
    private class SocketPair {
        public Socket client;
        public Socket server;
        
        public SocketPair() {
            // Create a server socket on the first available port.
            int defaultTestingPort = 13174;
            ServerSocket ss = null;
            do {
                try {
                    ss = new ServerSocket(defaultTestingPort);
                } catch(Exception e) {
                    ss = null;
                    defaultTestingPort++;
                }
            } while(ss == null);
            
            try {
                client = new Socket("127.0.0.1", defaultTestingPort);
                server = ss.accept();
            } catch(Exception e) {
                fail("Could not establish connection: " + e);
                e.printStackTrace();
            }
            
            assertNotNull(client);
            assertNotNull(server);            
        }
    }    
}