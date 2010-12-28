package apps.frontend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import player.request.factory.RequestFactory;
import player.request.grammar.AbortRequest;
import player.request.grammar.Request;
import player.request.grammar.StartRequest;
import player.request.grammar.StopRequest;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.logging.GamerLogger;
import util.match.Match;

/**
 * Frontend is an application that allows a number of gamer backends
 * to all play games from a single "frontend" address. Since each match
 * has a single unique ID associated with it, this frontend server can
 * route requests from the game server to the backend server associated
 * with that particular match.
 * 
 * TODO: This is a very naive implementation. It assumes that the backend
 * servers will never be busy with anything other than games served by this
 * frontend, and that they'll never crash or go into bad states. Furthermore,
 * it assumes that games always end with a STOP request, which won't be the
 * case in a web-based system if the user just navigates away from a page that
 * is serving a game.
 * 
 * @author Sam Schreiber
 */
public final class Frontend extends Thread
{
    /**
     * Backend encapsulates all of the information that we need to
     * uniquely identify a backend server, and determine whether we've
     * already allocated a game to it.
     * 
     * @author Sam
     */
    class Backend {
    	public int port;
    	public String ip;	
    	public boolean active;
    	
    	public Backend(String ip, int port) {
    		this.ip = ip;
    		this.port = port;
    		this.active = false;
    	}
    }
    
    /**
     * FrontendStatusReporter is a thread that periodically reports
     * how many frontends are connected to the backend. This information
     * is written to the standard output, and also logged in a logfile.
     * 
     * @author Sam
     */
    class FrontendStatusReporter extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    sleep(10000);
                    
                    int nBusy = 0;
                    for(Backend b : theBackends)
                        if(b.active)
                            nBusy++;
                    
                    GamerLogger.log("Frontend", "Current status: " + nBusy + "/" + theBackends.size() + " backends busy.");
                } catch(Exception e) {
                    GamerLogger.logStackTrace("Frontend", e);
                }
            }
        }
    }

    /**
     * BackendRegistrationThread is a thread that waits for backend servers
     * to register themselves with the frontend. Backend servers that want
     * this frontend to serve them games should register with this frontend
     * by sending a port number in an HTTP request to the registration port
     * on the frontend. The frontend will reply with "REGISTERED", and will
     * add that backend to the list of active backends.
     * 
     * @author Sam
     */    
    class BackendRegistrationThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted())
            {
                try
                {
                    Socket connection = backendRegistration.accept();
                    String in = HttpReader.readAsServer(connection);
                    String ip = connection.getInetAddress().getHostAddress();
                    GamerLogger.log("Frontend", "[Received at " + System.currentTimeMillis() + "] Registration: " + ip + " : " + in);
                    try {
                        int p = Integer.parseInt(in);
                        theBackends.add(new Backend(ip, p));
                        writeResponse(connection, "REGISTERED");
                    } catch(NumberFormatException e) {
                        GamerLogger.logStackTrace("Frontend", e);
                        writeResponse(connection, "FAILURE");
                    }
                }
                catch (Exception e)
                {
                    GamerLogger.logStackTrace("Frontend", e);
                }
            }
        }
    }
    
    // All of the information for tracking backends and match
    // backends to the games that they're playing.
    private Set<Backend> theBackends = new HashSet<Backend>();
    private Map<String, Backend> matchToBackendMap = new HashMap<String, Backend>();

    // Server socket for listening for connections from the real
    // game server, so they can be sent to backends.
    private ServerSocket backendRegistration;
    private ServerSocket listener;
    public Frontend(int port)
    {
        listener = null;
        while(listener == null) {
            try {
                listener = new ServerSocket(port);
            } catch (IOException ex) {
                listener = null;
                port++;
                System.err.println("Failed to start frontend on port: " + (port-1) + " trying port " + port);
            }				
        }
        
        // We absolutely require that the registration port be REGISTRATION_PORT,
        // because the backend servers will depend on that.
        try {
            backendRegistration = new ServerSocket(REGISTRATION_PORT);
        } catch (IOException ex) {
            backendRegistration = null;
            System.err.println("Failed to start backend registration on port: " + REGISTRATION_PORT);
            System.exit(1);
        }               
    }
    
    private Backend findAvailableBackend() {
    	for(Backend b : theBackends) {
    		if(!b.active)
    			return b;
    	}
    	return null;
    }
	
	@Override
	public void run()
	{
	    new FrontendStatusReporter().start();
	    new BackendRegistrationThread().start();	    
		
		while (!isInterrupted())
		{
			try
			{
				Socket connection = listener.accept();
				String in = HttpReader.readAsServer(connection);
				GamerLogger.log("Frontend", "[Received at " + System.currentTimeMillis() + "] " + in, GamerLogger.LOG_LEVEL_DATA_DUMP);

				Request request = new RequestFactory().create(null, in);
				String matchId = request.getMatchId();
				
				if (request instanceof StartRequest) {
					Backend b = findAvailableBackend();
					if (b == null) {
						// If there are no available backends,
						// respond with "busy".
						writeResponse(connection, "busy");
						continue;
					}

					// Start this match on a new backend, assuming that
					// we've found one that is available.
					b.active = true;
					matchToBackendMap.put(matchId, b);
				}
				Backend b = matchToBackendMap.get(matchId);
				if(b == null) {
                    writeResponse(connection, "busy");
                    continue;
				}
				
				// If we've reached this point, we know that we have a request
				// "in" that needs to be served to the backend "b", which has
				// been marked active (so it's now dedicated to this match).
				
				// Start a separate thread to open a connection to the backend
				// and wait for the response, and pass it back to the actual
				// server as soon as the backend responds.
				RequestHandler handler = new RequestHandler(connection, in, b, request instanceof StopRequest || request instanceof AbortRequest);
				handler.start();
			}
			catch (Exception e)
			{
				GamerLogger.logStackTrace("Frontend", e);
			}
		}
	}

	private static void writeResponse(Socket connection, String out) throws Exception {
		HttpWriter.writeAsServer(connection, out);
		connection.close();
		GamerLogger.log("Frontend", "[Sent at " + System.currentTimeMillis() + "] " + out, GamerLogger.LOG_LEVEL_DATA_DUMP);		
	}

	public static void main(String[] args)
	{
		Match fakeMatch = new Match("Frontend." + System.currentTimeMillis(), 0, 0, System.currentTimeMillis(), null);
		GamerLogger.startFileLogging(fakeMatch, "");		
		
		GamerLogger.setFileToDisplay("Frontend");
		Frontend theFrontend = new Frontend(9147);
		theFrontend.run();
	}
	
	// Calling this method will register yourself with the frontend at
	// "frontendAddress", indicating that you have a gamer available on
	// the port "myPort".
	public static final int REGISTRATION_PORT = 11111;
	public static void registerWithFrontend(String frontendAddress, int myPort, boolean orDie) {
	    try {
    	    Socket connection = new Socket(frontendAddress, REGISTRATION_PORT);
    	    HttpWriter.writeAsClient(connection, "" + myPort, "Backend");
    	    String status = HttpReader.readAsClient(connection);	    
    	    if (status.equals("REGISTERED")) {
    	        GamerLogger.log("GamePlayer", "Registered successfully with frontend server.");
    	    } else {
    	        GamerLogger.log("GamePlayer", "Failure to register with frontend server: " + status);
    	        if(orDie) System.exit(1);
    	    }
    	    connection.close();
	    } catch(Exception e) {
	        GamerLogger.logStackTrace("GamePlayer", e);
	        if(orDie) System.exit(1);
	    }
	}
}