package org.ggp.base.player.proxy;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.SocketException;

import org.ggp.base.util.logging.GamerLogger;


public class ProxyMessage implements Serializable {
    private static final long serialVersionUID = 1237859L;
    
    public final long messageCode;
    public final long receptionTime;
    public final String theMessage;
    
    public ProxyMessage(String theMessage, long messageCode, long receptionTime) {
        this.theMessage = theMessage;
        this.messageCode = messageCode;
        this.receptionTime = receptionTime;
    }
    
    public String toString() {
        return "ProxyMessage<" + messageCode + ", " + receptionTime + ">[\"" + theMessage + "\"]"; 
    }
    
    public static ProxyMessage readFrom(BufferedReader theInput) throws SocketException {        
        try {
            long messageCode = Long.parseLong(theInput.readLine());
            long receptionTime = Long.parseLong(theInput.readLine());
            String theMessage = theInput.readLine();
            return new ProxyMessage(theMessage, messageCode, receptionTime);
        } catch(SocketException se) {
            GamerLogger.log("Proxy", "[ProxyMessage Reader] Socket closed: stopping read operation.");
            throw se;
        } catch(Exception e) {
            GamerLogger.logStackTrace("Proxy", e);
            GamerLogger.logError("Proxy", "[ProxyMessage Reader] Could not digest message. Emptying stream.");
            try {
                // TODO: Fix this, I suspect it may be buggy.
                theInput.skip(Long.MAX_VALUE);
            } catch(SocketException se) {
                GamerLogger.log("Proxy", "[ProxyMessage Reader] Socket closed: stopping read operation.");
                throw se;                
            } catch(Exception ie) {
                GamerLogger.logStackTrace("Proxy", ie);
            }
            return null;
        }
    }
    
    public void writeTo(PrintStream theOutput) {
    	synchronized (theOutput) {
    		theOutput.println(messageCode);
    		theOutput.println(receptionTime);
    		theOutput.println(theMessage);
    	}
    }
}