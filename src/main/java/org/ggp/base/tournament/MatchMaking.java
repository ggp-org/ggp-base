package org.ggp.base.tournament;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.AggregateIterable;
import com.mongodb.Block;
import static com.mongodb.client.model.Sorts.*;
import static com.mongodb.client.model.Filters.*;
import org.bson.Document;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.nio.file.Files;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.io.IOException;
import java.lang.IllegalStateException;
import java.lang.RuntimeException;

class MatchMaking {

    private static final double INIT_SIGMA = 0.500;
    private static final double INIT_MU = 0.500;
    private static final double INIT_SKILL = 0.500;

    private static List<String> playersInTournament(MongoDatabase database, String tournament) {
        // "_id" required by API usage
        MongoCollection<Document> players = database.getCollection("players");
        List<Document> playersInTournament = 
            players.aggregate(
                asList(
                    new Document("$match", new Document("tournament", tournament)),
                    new Document("$group", new Document("_id", "$username"))
                    )).into(new ArrayList<Document>());

        List<String> users = new ArrayList<String>();
        for (Document aPlayer : playersInTournament)
            users.add(aPlayer.get("_id").toString());
        
        return users;
    }

    private static List<String> latestRanksInTournament(MongoDatabase database, String tournament) {
        MongoCollection<Document> matches = database.getCollection("matches");
        Document latestMatch = matches.find(
            eq("tournament", tournament)).sort(descending("createdAt")).first();

        if (latestMatch == null) 
            return playersInTournament(database, tournament);

        List<Document> currentRanks = (List<Document>)latestMatch.get("currentRanks");
        List<String> users = new ArrayList<String>();
        for (Document aRank : currentRanks)
            users.add(aRank.get("username").toString());
        
        return users;
    }

    public static void startMatch() {
        
    }

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient( "localhost" , 3001 );
        MongoDatabase database = mongoClient.getDatabase("meteor");
        MongoCollection<Document> tournaments = database.getCollection("tournaments");

        Document tour = tournaments.find().sort(descending("createdAt")).first();
        System.out.println("===>  tournament = " + tour.get("name").toString());
        
        for (String aUser : latestRanksInTournament(database, tour.get("name").toString())) {
            System.out.println("user = " + aUser);
        }

        String tour = tour.get("name").toString();
        List<String> users = latestRanksInTournament(database, tour);
         // = matchMaking(tour, users)
        // while (true) {
            // get users

            // match users
            // aMatch = {tour, [{username}]}
            // remove playing users from users
            // Document aMatch =  matchMaking(tour, users);

            // play
            // results = [{user, skill, sigma, mu, etc}] 
            // Document results = startMatch(aMatch)

            // update skills
            // updateSkills(tour, users, results);
            // push playedUser back into users
        // }

    }
}