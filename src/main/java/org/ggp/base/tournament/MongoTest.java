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
        // MongoClient mongoClient = new MongoClient( "localhost" , 3001 );
        // MongoDatabase database = mongoClient.getDatabase("meteor");
        
        Document doc = new Document("name", "MongoDB")
               .append("type", "database")
               .append("count", 1)
               .append("info", new Document("x", 203).append("y", 102))
               .append("myArray", Arrays.asList(new Document("x", 203).append("y", 102),
                                                new Document("x", 33).append("y", 222)));
        System.out.println("=================>  " + doc.toJson());
        // mongoClient.close();
    }
}