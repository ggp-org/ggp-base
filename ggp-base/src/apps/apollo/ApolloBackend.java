package apps.apollo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
 * SAMPLE INVOCATION (when running locally):
 * 
 * ResourceLoader.load_raw('http://127.0.0.1:9124/' + escape(JSON.stringify({"playClock":5,
 * "startClock":5, "gameURL":"http://ggp-repository.appspot.com/games/connectFour/",
 * "matchId":"apollo.sample_2", "players":["127.0.0.1:3333", "ggp-webplayer.appspot.com:80"]})))
 * 
 * Apollo Backend Server replies with the URL of the match on the spectator server.
 * 
 * @author Sam Schreiber
 */
public final class ApolloBackend
{
    public static final int SERVER_PORT = 9124;
    
    // Matches are run asynchronously in their own threads.
    static class RunMatchThread extends Thread {
        int playClock, startClock;
        String gameURL, matchId;
        List<String> names, hosts;
        List<Integer> ports;
        
        Game theGame;
        Match theMatch;
        GameServer theServer;
        
        private static final String spectatorServerURL = "http://ggp-spectator.appspot.com/";
        
        public RunMatchThread(Socket connection) throws IOException, JSONException {
            System.out.println("Got connection from client.");
            
            String line = HttpReader.readAsServer(connection);
            
            System.out.println("Read line from client: " + line);
            
            JSONObject theJSON = new JSONObject(line);
            playClock = theJSON.getInt("playClock");
            startClock = theJSON.getInt("startClock");
            gameURL = theJSON.getString("gameURL");                
            matchId = theJSON.getString("matchId");

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

            // Get the match into a state where we can publish it to
            // the spectator server, so that we have a spectator server
            // URL to return for this request.
            theGame = RemoteGameRepository.loadSingleGame(gameURL);            
            theMatch = new Match(matchId, startClock, playClock, theGame);  
            theServer = new GameServer(theMatch, hosts, ports, names);
            String theSpectatorURL = theServer.startPublishingToSpectatorServer(spectatorServerURL);               

            HttpWriter.writeAsServer(connection, spectatorServerURL + "matches/" + theSpectatorURL + "/");
            connection.close();
        }
        
        @Override
        public void run() {
            System.out.println("Starting match: " + matchId);
            theServer.start();            
            try {
                theServer.join();
            } catch (InterruptedException e) {
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