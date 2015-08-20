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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.*;

import static java.util.Arrays.asList;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.lang.IllegalStateException;
import java.lang.RuntimeException;

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


class MatchMaking {

    public MatchMaking() {}

    public static List<String> randomlyPickPlayers(List<String> avialableUsers, int numPlayers) {
        long seed = System.nanoTime();
        Collections.shuffle(avialableUsers, new Random(seed));
        List<String> pickedUsers = new ArrayList<String>();
        for (int i = 0; i < numPlayers; ++i ) 
            pickedUsers.add(avialableUsers.remove(0));
        return pickedUsers;
    }

    public static void main(String[] args) {

        TournamentManager tm = new TournamentManager("Tic");
//        TournamentManager tm = new TournamentManager("FreeForAll");
        long numberOfRandomMatches = 0;
        long MAX_NUM_RANDOM_MATCHES = 5;

        // Start loop
        try {
            while (true) {
                System.out.println("Start loop....");
                // pick random if no match yet.
                int numPlayers = 2;
                System.out.println("number of users in this tournament = " + tm.getAvailableUsers().size());
                if (tm.getAvailableUsers().size() >= numPlayers) {

                    if (MAX_NUM_RANDOM_MATCHES > numberOfRandomMatches || tm.numberOfMatches() < MAX_NUM_RANDOM_MATCHES) {
                        System.out.println("Yes, We can play!");
                        System.out.println("numberOfRandomMatches = " + numberOfRandomMatches);
                        // choose players randomly for N matches
                        System.out.println("Start match!");
                        List<String> pickedUsers =
                                randomlyPickPlayers(new ArrayList<String>(tm.getAvailableUsers()), tm.numberOfPlayers());
                        tm.playOneVsOne(pickedUsers);
                        numberOfRandomMatches++;

                    } else {
                        // get two least played user and both produce good match quality
                        System.out.println("Yes, matchByQuality!");
                        tm.matchByQuality();
                    }
                }
                Thread.sleep(2 * 1000);
            }
        }
        catch (InterruptedException itre) {
            itre.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}