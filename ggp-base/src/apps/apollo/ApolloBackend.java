package apps.apollo;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

import server.GameServer;
import util.game.Game;
import util.game.RemoteGameRepository;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.match.Match;

/**
 * The Apollo Backend Server is a multi-threaded web server that runs matches
 * and reports back match information on behalf of remote clients. It serves as
 * a backend for intermediary systems that, due to restrictions on the length of
 * HTTP connections, cannot run matches themselves.
 * 
 * If you don't know what the GGP Apollo project is, this system is not likely
 * going to be interesting to you. Feel free to ignore it.
 * 
 * @author Sam Schreiber
 */
public final class ApolloBackend
{
    public static final int SERVER_PORT = 9124;
    
    // Matches are run asynchronously in their own threads.
    static class RunMatchThread extends Thread {
        int playClock, startClock;
        String gameURL, matchId, callbackURL;
        List<String> names, hosts;
        List<Integer> ports;
        
        public RunMatchThread(Socket connection) throws IOException, JSONException {
            System.out.println("Got connection from client.");
            
            String line = HttpReader.readAsServer(connection);
            HttpWriter.writeAsServer(connection, "Starting match.");            
            connection.close();
            
            System.out.println("Read line from client: " + line);
            
            JSONObject theJSON = new JSONObject(line);
            playClock = theJSON.getInt("playClock");
            startClock = theJSON.getInt("startClock");
            gameURL = theJSON.getString("gameURL");                
            matchId = theJSON.getString("matchId");
            callbackURL = theJSON.getString("callbackURL");

            names = new ArrayList<String>();
            hosts = new ArrayList<String>();            
            ports = new ArrayList<Integer>();
            JSONArray thePlayers = theJSON.getJSONArray("players");
            for (int i = 0; i < thePlayers.length(); i++) {
                String[] splitAddress = thePlayers.getString(i).split(":");
                hosts.add(splitAddress[0]);
                ports.add(Integer.parseInt(splitAddress[1]));
                names.add("");
            }
        }
        
        @Override
        public void run() {
            System.out.println("Starting match: " + matchId);
            
            Game theGame = RemoteGameRepository.loadSingleGame(gameURL);            
            Match match = new Match(matchId, startClock, playClock, theGame);  
            
            GameServer gameServer = new GameServer(match, hosts, ports, names);
            gameServer.start();
            try {
                gameServer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            
            System.out.println("Completed match: " + matchId + ". POSTing response to callback URL: " + callbackURL);

            // POST a response to the Callback URL
            try {
                URL theURL = new URL(callbackURL);
                URLConnection conn = theURL.openConnection();
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(match.toJSON());
                wr.flush();
                wr.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
    
    public static void main(String[] args) {
        ServerSocket listener = null;
        try {
             listener = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            System.err.println("Could not open server on port " + SERVER_PORT + ": " + e);
            e.printStackTrace();
            return;
        }
        
        while (true) {
            try {
                Socket connection = listener.accept();
                Thread handlerThread = new RunMatchThread(connection);
                handlerThread.start();
            } catch (Exception e) {
                System.err.println(e);
            }
        }        
    }
}