package org.ggp.base.tournament;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Filters.*;
import org.bson.Document;

class MongoTest {

    public static void updateTournament(
        MongoCollection<Document> tournaments, 
        Document player) 
    {
        System.out.println("tournament id = " + player.get("latestTourId"));
        tournaments.updateOne(
            eq("_id", player.get("latestTourId")), 
            new Document("$push", new Document("players", player.get("_id"))));
    }

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient( "localhost" , 3001 );
        MongoDatabase database = mongoClient.getDatabase("meteor");
        MongoCollection<Document> players = database.getCollection("players");
        MongoCollection<Document> tournaments = database.getCollection("tournaments");

        Document playerToAdd = 
            players.find(eq("status", "not compiled")).sort(descending("createdAt")).first();
        if (playerToAdd != null) {
            System.out.println("player to add = " + playerToAdd.get("_id"));
            updateTournament(tournaments, playerToAdd);   
        } else {
            System.out.println("no player to add = ");
        }
        
        mongoClient.close();
    }
}