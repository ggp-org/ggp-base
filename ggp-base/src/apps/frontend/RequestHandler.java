package apps.frontend;

import java.net.Socket;

import util.http.HttpReader;
import util.http.HttpWriter;
import util.logging.GamerLogger;

/**
 * RequestHandler is a separate thread for the frontend that actually handles
 * proxying a request from the game server to the backend player. This is done
 * in a separate thread so that waiting for the backend player to play doesn't
 * block the rest of the frontend.
 * 
 * @author Sam
 */
public class RequestHandler extends Thread {
	private Frontend.Backend backend;
	private Socket connection;
	private String input;	
	private boolean unmarkWhenDone;
	
	public RequestHandler(Socket connection, String in, Frontend.Backend b, boolean unmarkWhenDone) {
		this.unmarkWhenDone = unmarkWhenDone;
		this.connection = connection;
		this.backend = b;
		this.input = in;
	}    	

	public void run() {
		try {
			Socket s = new Socket(backend.ip, backend.port);
			HttpWriter.writeAsClient(s, input, "Frontend");
			
			// NOTE: This effectively strips out the player name
			// from the HTTP messages. This will need to be fixed if
			// the player name (which the server sends to the client)
			// gets used at some point.
			String out = HttpReader.readAsClient(s);
			s.close();
			
			HttpWriter.writeAsServer(connection, out);
			connection.close();
			
			if (unmarkWhenDone) {
				backend.active = false;
			}
		} catch(Exception e) {
			GamerLogger.logStackTrace("Frontend", e);
		}
	}
}