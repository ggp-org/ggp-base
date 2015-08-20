package org.ggp.base.tournament;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PopulateGames {
    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient("localhost" , 3001);
        MongoDatabase database = mongoClient.getDatabase("meteor");
        MongoCollection<Document> matches = database.getCollection("matches");
        MongoCollection<Document> tournaments = database.getCollection("tournaments");
        MongoCollection<Document> players = database.getCollection("players");
        MongoCollection<Document> games = database.getCollection("games");

        GameRepository theRepository = GameRepository.getDefaultRepository();

        List<String> theKeyList = new ArrayList<String>(theRepository.getGameKeys());
        Collections.sort(theKeyList);

        List<Document> gamesToAdd = new ArrayList<>();
        for (String theKey : theKeyList) {
            Game theGame = theRepository.getGame(theKey);
            if (theGame == null) {
                continue;
            }
            String theName = theGame.getName();
            if (theName == null) {
                theName = theKey;
            }
            if (theName.length() > 24)
                theName = theName.substring(0, 24) + "...";

            Document aGame = new Document("name", theName)
                    .append("key", theKey);
            gamesToAdd.add(aGame);
        }

        System.out.println("Number of games = " + gamesToAdd.size());
        if (games.count() == 0)
            games.insertMany(gamesToAdd);
        mongoClient.close();
    }
}
