package org.ggp.base.tournament;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Filters.*;
import org.bson.Document;

import java.util.Arrays;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;

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

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient( "localhost" , 3001 );
        MongoClient anotherMongoClient = new MongoClient( "localhost" , 3001 );
        
        MongoDatabase database = mongoClient.getDatabase("meteor");
        MongoDatabase anotherDatabase = anotherMongoClient.getDatabase("meteor");

        // Document doc = new Document("name", "MongoDB")
        //        .append("type", "database")
        //        .append("count", 1)
        //        .append("info", new Document("x", 203).append("y", 102))
        //        .append("myArray", Arrays.asList(new Document("x", 203).append("y", 102),
        //                                         new Document("x", 33).append("y", 222)));
        // System.out.println("=================>  " + doc.toJson());

        // MongoCollection<Document> tournaments = database.getCollection("tournaments");
        // Document tournament = tournaments.find(eq("name", "Money")).first();
        
        // MongoCollection<Document> anotherTournaments = anotherDatabase.getCollection("tournaments");
        // Document anotherTournament = tournaments.find(eq("name", "Money")).first();
        
        // System.out.println("Before Update: Money = " 
        //     + tournament.get("game") + ", " + tournament.get("status"));

        // tournaments.updateOne(eq("name", "Money"), 
        //     new Document("$set", new Document("game", "Doo").append("status", "See")));

        // System.out.println("After Update: Money = " + anotherTournament.get("game") + ", " + anotherTournament.get("status"));


        MongoCollection<Document> players = database.getCollection("players");
        Document aPlayer = players.find(eq("status", "compiled")).first();
        String pathToClasses = aPlayer.get("pathToClasses").toString();

        try {
            
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
                    Gamer gamer = (Gamer) aClass.newInstance();
                    int port = 9147;
                    GamePlayer aGamePlayer = new GamePlayer(port, gamer);
                    aGamePlayer.start();
                    // aGamePlayer.shutdown();
                    return;
                }
            }

            

        } catch (Exception e) {
            e.printStackTrace();
        }

        mongoClient.close();
    }
}