package apps.tiltyard;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import util.gdl.grammar.GdlPool;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.match.Match;
import util.symbol.grammar.SymbolPool;

/**
 * The Tiltyard Backend Server is a multi-threaded web server that runs matches
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
 * "matchId":"tiltyard.sample_2", "players":["127.0.0.1:3333", "player.ggp.org:80"],
 * "playerNames":["GreenShell","RedShell"]})))
 * 
 * Tiltyard Backend Server replies with the URL of the match on the spectator server.
 * 
 * You shouldn't be running this server unless you are bringing up an instance of the
 * online GGP.org Tiltyard or an equivalent service.
 * 
 * @author Sam Schreiber
 */
public final class TiltyardBackend
{
    public static final int SERVER_PORT = 9124;
    private static final String spectatorServerURL = "http://matches.ggp.org/";
    private static final String registrationURL = "http://tiltyard.ggp.org/backends/register";
    
    private static Integer ongoingMatches = new Integer(0);

    static EncodedKeyPair getKeyPair(String keyPairString) {
    	if (keyPairString == null)
    		return null;
        try {
            return new EncodedKeyPair(keyPairString);
        } catch (JSONException e) {
            return null;
        }
    }
    public static final EncodedKeyPair theTiltyardKeys = getKeyPair(FileUtils.readFileAsString(new File("src/apps/tiltyard/TiltyardKeys.json")));
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
    
    // Matches are run asynchronously in their own threads.
    static class RunMatchThread extends Thread {
        int playClock, startClock;
        String gameURL, matchId;
        List<String> names, hosts;
        List<Integer> ports;
        
        Game theGame;
        Match theMatch;
        GameServer theServer;
                
        public RunMatchThread(Socket connection, Map<String, String> knownMatches) throws IOException, JSONException {
            String line = HttpReader.readAsServer(connection);
            System.out.println("On " + new Date() + ", client has requested: " + line);
            
            String response = null;
            if (line.equals("ping")) {
                response = generateSignedPing();
            } else {
                JSONObject theJSON = new JSONObject(line);
                playClock = theJSON.getInt("playClock");
                startClock = theJSON.getInt("startClock");
                gameURL = theJSON.getString("gameURL");                
                matchId = theJSON.getString("matchId");
                
                synchronized (knownMatches) {
	                if (knownMatches.containsKey(matchId)) {
	                    response = knownMatches.get(matchId);
	                } else {
		                names = new ArrayList<String>();
		                hosts = new ArrayList<String>();            
		                ports = new ArrayList<Integer>();
		                JSONArray thePlayers = theJSON.getJSONArray("players");
		                JSONArray thePlayerNames = theJSON.getJSONArray("playerNames");
		                for (int i = 0; i < thePlayers.length(); i++) {
		                    String playerAddress = thePlayers.getString(i);
		                    if (playerAddress.startsWith("http://")) {
		                        playerAddress = playerAddress.replace("http://", "");
		                    }
		                    if (playerAddress.endsWith("/")) {
		                        playerAddress = playerAddress.substring(0, playerAddress.length()-1);
		                    }
		                    String[] splitAddress = playerAddress.split(":");
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

		                // Force any players named "random" to play randomly. Such players
		                // are added to the rotation by the Tiltyard scheduler when real players
		                // aren't available to fill a spot in a particular match.
		                for (int i = 0; i < names.size(); i++) {
		                	if (names.get(i).toLowerCase().equals("random")) {
		                		theServer.makePlayerPlayRandomly(i);
		                	}
		                }
		                
		                // Limit the rate at which the match advances, to avoid overloading
		                // the players and the spectator server with many requests.
		                theServer.setForceUsingEntireClock();
		                
		                response = spectatorServerURL + "matches/" + theSpectatorURL + "/";
		                knownMatches.put(matchId, response);
	                }
                }
            }

            HttpWriter.writeAsServer(connection, response);
            connection.close();
        }
        
        @Override
        public void run() {
            if (theServer != null) {                
                synchronized (ongoingMatches) {
                	ongoingMatches++;
                }
                System.out.println("On " + new Date() + ", starting match: " + matchId + ". There are now " + ongoingMatches + " ongoing matches.");
                theServer.start();
                try {
                    theServer.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }                
                synchronized (ongoingMatches) {
                	ongoingMatches--;                	
                	if (ongoingMatches == 0) {
                		GdlPool.drainPool();
                		SymbolPool.drainPool();
                		System.gc();
                		System.out.println("On " + new Date() + ", completed match: " + matchId + ". Pools drained since there are no ongoing matches.");
                	} else {
                		System.out.println("On " + new Date() + ", completed match: " + matchId + ". There are now " + ongoingMatches + " ongoing matches.");
                	}
                }
            }
        }
    }
    
    static class TiltyardRegistration extends Thread {
        @Override
        public void run() {
            // Send a registration ping to Tiltyard every five minutes.
            while (true) {
                try {
                    RemoteResourceLoader.postRawWithTimeout(registrationURL, generateSignedPing(), 2500);                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        if (theTiltyardKeys == null) {
            System.err.println("Could not load cryptographic keys for signing Tiltyard matches.");
            return;
        }

        GdlPool.caseSensitive = false;
        new TiltyardRegistration().start();
        
        Map<String, String> knownMatches = new HashMap<String, String>();
        while (true) {
            try {
                Socket connection = listener.accept();
                RunMatchThread handlerThread = new RunMatchThread(connection, knownMatches);
                handlerThread.start();
            } catch (Exception e) {
                System.err.println(e);
            }
        }        
    }
}