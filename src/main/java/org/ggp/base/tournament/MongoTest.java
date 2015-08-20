package org.ggp.base.tournament;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Filters.*;
import org.bson.Document;

import java.util.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import java.nio.file.Files;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.server.GameServer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import jskills.*;
import jskills.trueskill.TwoPlayerTrueSkillCalculator;

class MongoTest {

    public static void updateTournament(
        MongoCollection<Document> tournaments, Document player) 
    {
        System.out.println("tournament id = " + player.get("latestTourId"));
        tournaments.updateOne(
            eq("_id", player.get("latestTourId")), 
            new Document("$push", new Document("players", player.get("_id"))));
    }

    public static void testAddPlayers(MongoDatabase database) {
        MongoCollection<Document> players = database.getCollection("players");
        MongoCollection<Document> tournaments = database.getCollection("tournaments");
        Document playerToAdd = 
            players.find(eq("status", "not compiled")).sort(descending("createdAt")).first();

        if (playerToAdd != null) {
            
            Document tour = tournaments.find(eq("name", playerToAdd.get("latestTourName"))).first();
            String latestTourName = playerToAdd.get("latestTourName").toString();
            String playerName = playerToAdd.get("name").toString();
            Document isPlayerAdded = 
                tournaments.find(
                    and( 
                        eq("name", latestTourName),
                        elemMatch("players", eq("name", playerName))
                    )).first();

            if (isPlayerAdded != null) {
                System.out.println("This player is already in the tournament!");
            } else {
                System.out.println("Ready to add new player");
                System.out.println("tournament name = " + tour.get("name"));
                System.out.println("Player to add = " + playerName);
                
                Document newPlayer = new Document("name", playerName);

                tournaments.updateOne(
                    eq("name", latestTourName), 
                    new Document("$push", new Document("players", newPlayer)));
            }

        } else {
            System.out.println("no player to add!");
        }
    }

    public static void playGame(List<String> hostNames, 
                                List<String> playerNames, 
                                List<Integer> portNumbers, 
                                String gameKey,
                                GamePlayer aGamePlayer) throws IOException, InterruptedException {
        
        Game game = GameRepository.getDefaultRepository().getGame(gameKey);
        int expectedRoles = Role.computeRoles(game.getRules()).size();
        if (hostNames.size() != expectedRoles) {
            throw new RuntimeException("Invalid number of players for game " + gameKey + ": " + hostNames.size() + " vs " + expectedRoles);
        }

        String tourneyName = "testPlayGame";
        String matchName = tourneyName + "." + gameKey + "." + System.currentTimeMillis();
        int startClock = 3;
        int playClock = 14;
        Match match = new Match(matchName, -1, startClock, playClock, game);
        match.setPlayerNamesFromHost(playerNames);

        // Actually run the match, using the desired configuration.
        aGamePlayer.start();
        GameServer server = new GameServer(match, hostNames, portNumbers);
        server.start();
        server.join();
        aGamePlayer.shutdown();

        // Open up the directory for this tournament.
        // Create a "scores" file if none exists.
        File f = new File(tourneyName);
        if (!f.exists()) {
            f.mkdir();
            // f = new File(tourneyName + "/scores");
            f.createNewFile();
        }

        // Open up the JSON file for this match, and save the match there.
        f = new File(tourneyName + "/" + matchName + ".json");
        if (f.exists()) f.delete();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(match.toJSON());
        bw.flush();
        bw.close();
        
    }

    public static GamePlayer gamePlayer(Document aPlayer) throws Exception {
        String pathToClasses = aPlayer.get("pathToClasses").toString();
        URL url = new File(pathToClasses).toURL();
        URL[] urls = new URL[]{url};
        ClassLoader cl = new URLClassLoader(urls);
        String[] extensions = {"class"};
        String packageName = new File(pathToClasses).listFiles()[0].getName();
        String pathToPackage = pathToClasses + "/" + packageName;
        Collection<File> allClassFiles = 
            FileUtils.listFiles(new File(pathToPackage), extensions, false);
        
        // Loop through all class files to find Gamer class.
        for (Iterator<File> it = allClassFiles.iterator(); it.hasNext();) {
            File f = it.next();
            System.out.println("File name = " + f.getName());
            String playerName = f.getName().split("\\.(?=[^\\.]+$)")[0];
            String playerPackage = packageName + "." + playerName;
            Class aClass = cl.loadClass(playerPackage);
            
            System.out.println("...........detecting gamer");
            // found one and update player name, status and path to classes.
            if (Gamer.class.isAssignableFrom(aClass)) {
                System.out.println( f.getName() 
                    + " is a player!!, and it is " + aClass.getSimpleName());
                // Setup players
                Gamer gamer = (Gamer) aClass.newInstance();
                int port = 9147;
                GamePlayer aGamePlayer = new GamePlayer(port, gamer);
                return aGamePlayer;
            }
        }
        return null;
    }

    private static void prepareMatch() {

        // Play game!
        // List<String> hostNames = new ArrayList<String>();
        // List<String> userNames = new ArrayList<®String>();
        // hostNames.add("localhost");
        // userNames.add("utv");
        // portNumbers.add(port);
        // String gameKey = "hex";
        // System.out.println("........ Start game!");
        // playGame(hostNames, userNames, portNumbers, gameKey, aGamePlayer);
        // System.out.println("........ End game!");
        // List<Integer> portNumbers = new ArrayList<Integer>();
    }

    public static void main(String[] args) {

        TournamentManager tm = new TournamentManager("TicTacToe");
        System.out.println(tm.getTournamentName());

        try {

//            List<String> users = tm.usersInTournament();
//            long seed = System.nanoTime();
//            Collections.shuffle(users, new Random(seed));
//            tm.playOneVsOne(users.subList(0, 2));
//            System.out.println(" Hey ....");

//            List<Document> userNames = new ArrayList<>();
//            for (int i = 0; i < 3; i++) {
//                userNames.add(new Document("username", Integer.toString(i)).append("score", 10));
//            }
//
//            Document newRanks = new Document("result", userNames);
//            System.out.println(newRanks.toJson());

        } catch (Exception e) {
            e.printStackTrace();
            tm.shutdown();
        }
    }
}
