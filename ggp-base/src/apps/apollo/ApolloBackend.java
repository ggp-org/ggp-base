package apps.apollo;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

import server.GameServer;
import util.configuration.RemoteResourceLoader;
import util.crypto.SignableJSON;
import util.crypto.BaseCryptography.EncodedKeyPair;
import util.files.FileUtils;
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
 * This is the backend for the continuously-running online GGP.org Tiltyard,
 * which schedules matches between players around the world and aggregates stats
 * based on the outcome of those matches.
 * 
 * SAMPLE INVOCATION (when running locally):
 * 
 * ResourceLoader.load_raw('http://127.0.0.1:9124/' + escape(JSON.stringify({"playClock":5,
 * "startClock":5, "gameURL":"http://games.ggp.org/base/games/connectFour/",
 * "matchId":"apollo.sample_2", "players":["127.0.0.1:3333", "player.ggp.org:80"],
 * "playerNames":["GreenShell","RedShell"]})))
 * 
 * Apollo Backend Server replies with the URL of the match on the spectator server.
 * 
 * @author Sam Schreiber
 */
public final class ApolloBackend
{
    public static final int SERVER_PORT = 9124;

    static EncodedKeyPair getKeyPair(String keyPairString) {
        try {
            return new EncodedKeyPair(keyPairString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static final EncodedKeyPair theTiltyardKeys = getKeyPair(FileUtils.readFileAsString(new File("src/apps/apollo/ApolloKeys.json")));
    
    private static final String registrationURL = "http://tiltyard.ggp.org/backends/register";
    private static final String spectatorServerURL = "http://matches.ggp.org/";
    
    // Matches are run asynchronously in their own threads.
    static class RunMatchThread extends Thread {
        int playClock, startClock;
        String gameURL, matchId;
        List<String> names, hosts;
        List<Integer> ports;
        
        Game theGame;
        Match theMatch;
        GameServer theServer;
                
        public RunMatchThread(Socket connection) throws IOException, JSONException {
            String line = HttpReader.readAsServer(connection);

            String response = null;
            if (line.equals("ping")) {
                response = generateSignedPing();
            } else {
                System.out.println("On " + new Date() + ", client has requested: " + line);
                
                JSONObject theJSON = new JSONObject(line);
                playClock = theJSON.getInt("playClock");
                startClock = theJSON.getInt("startClock");
                gameURL = theJSON.getString("gameURL");                
                matchId = theJSON.getString("matchId");
    
                names = new ArrayList<String>();
                hosts = new ArrayList<String>();            
                ports = new ArrayList<Integer>();
                JSONArray thePlayers = theJSON.getJSONArray("players");
                JSONArray thePlayerNames = theJSON.getJSONArray("playerNames");
                for (int i = 0; i < thePlayers.length(); i++) {
                    String[] splitAddress = thePlayers.getString(i).split(":");
                    // TODO(schreib): Fix this so it can handle "http://" prefix.
                    hosts.add(splitAddress[0]);
                    ports.add(Integer.parseInt(splitAddress[1]));
                    names.add(thePlayerNames.getString(i));
                }
                
                // Get the match into a state where we can publish it to
                // the spectator server, so that we have a spectator server
                // URL to return for this request.
                theGame = RemoteGameRepository.loadSingleGame(gameURL);            
                theMatch = new Match(matchId, startClock, playClock, theGame);
                theMatch.setCryptographicKeys(theTiltyardKeys);
                theMatch.setPlayerNamesFromHost(names);
                theServer = new GameServer(theMatch, hosts, ports, names);
                String theSpectatorURL = theServer.startPublishingToSpectatorServer(spectatorServerURL);
                
                // Limit the rate at which the match advances, to avoid overloading
                // the players and the spectator server with many requests.
                theServer.setForceUsingEntireClock();
                
                response = spectatorServerURL + "matches/" + theSpectatorURL + "/";
            }

            HttpWriter.writeAsServer(connection, response);
            connection.close();
        }
        
        @Override
        public void run() {
            System.out.println("On " + new Date() + ", starting match: " + matchId);
            theServer.start();
            try {
                theServer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("On " + new Date() + ", completed match: " + matchId);
        }
    }
    
    public static String generateSignedPing() {
        JSONObject thePing = new JSONObject();
        try {        
            thePing.put("lastTimeBlock", (System.currentTimeMillis() / 3600000));
            thePing.put("nextTimeBlock", (System.currentTimeMillis() / 3600000)+1);
            SignableJSON.signJSON(thePing, theTiltyardKeys.thePublicKey, theTiltyardKeys.thePrivateKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return thePing.toString();
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

        try {
            RemoteResourceLoader.postRawWithTimeout(registrationURL, generateSignedPing(), 2500);
        } catch (IOException e) {
            System.err.println("Could not register with Tiltyard Scheduler.");
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