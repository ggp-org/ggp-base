package org.ggp.base.tournament;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Sorts.*;
import static com.mongodb.client.model.Filters.*;

import org.bson.Document;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.*;

import static java.util.Arrays.asList;

import java.net.URL;
import java.net.URLClassLoader;

import java.lang.RuntimeException;

import org.bson.types.ObjectId;
import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.server.GameServer;

import org.ggp.base.server.event.ServerMatchUpdatedEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Role;

import jskills.*;
import jskills.trueskill.TwoPlayerTrueSkillCalculator;

public class TournamentManager implements Observer {
    private TwoPlayerTrueSkillCalculator twoPlayersCalculator;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> games;
    private MongoCollection<Document> matches;
    private MongoCollection<Document> tournaments;
    private MongoCollection<Document> players;
    private final Map<String, List<GamePlayer>> playerMap = new HashMap<>();
    private final Set<String> availableUsers = new HashSet<>();
    private String tournament;
    private String gameKey;


    public enum GAME_TYPE {
        SINGLE, ONE_VS_ONE, FOUR_FREE_FOR_ALL;
    }

    public TournamentManager() {
        twoPlayersCalculator = new TwoPlayerTrueSkillCalculator();
        mongoClient = new MongoClient("localhost" , 3001);
        database = mongoClient.getDatabase("meteor");
        games = database.getCollection("games");
        matches = database.getCollection("matches");
        tournaments = database.getCollection("tournaments");
        players = database.getCollection("players");

    }

    public TournamentManager(String touneyName) {
        twoPlayersCalculator = new TwoPlayerTrueSkillCalculator();
        mongoClient = new MongoClient("localhost" , 3001);
        database = mongoClient.getDatabase("meteor");
        matches = database.getCollection("matches");
        tournaments = database.getCollection("tournaments");
        players = database.getCollection("players");
        games = database.getCollection("games");
        tournament = touneyName;
        String gameName = tournaments.find(eq("name", tournament)).first().getString("game");
        System.out.println("-----> " + gameName);
        System.out.println("-----> " + games.find(eq("name", gameName)).first());
        gameKey = games.find(eq("name", gameName)).first().getString("key");
        System.out.println("game key -----> " + gameKey);
        availableUsers.addAll(usersInTournament());
    }

    public void setTournamentName(String name) {
        this.tournament = name;
    }
    public String getTournamentName() {
        return this.tournament;
    }

    public Set<String> getAvailableUsers() {
        return availableUsers;
    }

    public void shutdown() {
        mongoClient.close();
    }

    // Magic number
    public int numberOfPlayers() {
        // return tournaments.find(eq("tournament", tournament)).first().getInteger("numPlayers");
        return 2;
    }

    public long numberOfMatches() {
        return matches.count(eq("tournament", tournament));
    }

    public void matchByQuality() throws Exception {
        System.out.println("------- matchByQuality ---------");
        List<Document> ranks = (List < Document >)latestMatch(tournament).get("ranks");

        Collections.sort(ranks, new Comparator<Document>() {
            public int compare(Document d1, Document d2) {
                return d1.getInteger("numMatch").compareTo(d2.getInteger("numMatch"));
            }
        });

        for (int i = 0; i < ranks.size() - 1; i++) {
            for (int j = i + 1; j < ranks.size(); j++) {
                Player<String> player1 = new Player<String>(ranks.get(i).getString("username"));
                Player<String> player2 = new Player<String>(ranks.get(j).getString("username"));
                Rating p1Rating = new Rating(ranks.get(i).getDouble("mu"), ranks.get(i).getDouble("sigma"));
                Rating p2Rating = new Rating(ranks.get(j).getDouble("mu"), ranks.get(j).getDouble("sigma"));
                Team team1 = new Team(player1, p1Rating);
                Team team2 = new Team(player2, p2Rating);
                Collection<ITeam> teams = Team.concat(team1, team2);
                if (twoPlayersCalculator.calculateMatchQuality(GameInfo.getDefaultGameInfo(), teams) > 0.5) {
                    System.out.println("------- play a match by quality ---------");
                    List<String> pickedUsers = new ArrayList<>();
                    pickedUsers.add(player1.getId());
                    pickedUsers.add(player2.getId());
                    playOneVsOne(pickedUsers);
                    return;
                }
            }
        }

        // if no match, match a player having least number of matches to a random player
        Random rand = new Random();
        int max = ranks.size() - 1;
        int randomNum = rand.nextInt((max - 1) + 1) + 1;

        Player<String> player1 = new Player<String>(ranks.get(0).getString("username"));
        Player<String> player2 = new Player<String>(ranks.get(randomNum).getString("username"));
        Rating p1Rating = new Rating(ranks.get(0).getDouble("mu"), ranks.get(0).getDouble("sigma"));
        Rating p2Rating = new Rating(ranks.get(randomNum).getDouble("mu"), ranks.get(randomNum).getDouble("sigma"));
        Team team1 = new Team(player1, p1Rating);
        Team team2 = new Team(player2, p2Rating);
        Collection<ITeam> teams = Team.concat(team1, team2);
        List<String> pickedUsers = new ArrayList<>();
        pickedUsers.add(player1.getId());
        pickedUsers.add(player2.getId());
        playOneVsOne(pickedUsers);
    }

    public String latestTournament() {
        Document tour = tournaments.find().sort(descending("createdAt")).first();
        return tour.get("name").toString();
    }

    private List<String> usersInTournament() {
        // "_id" required by API usage
        List<Document> playersInTournament =
                players.aggregate(
                        asList(
                                new Document("$match", new Document("tournament", tournament)),
                                new Document("$group", new Document("_id", "$username"))
                        )).into(new ArrayList<Document>());

        List<String> users = new ArrayList<String>();
        for (Document aPlayer : playersInTournament) {
            String username = aPlayer.get("_id").toString();
            // System.out.println("username = " + username);
            Document thisPlayer =
                    players.find(and(
                            eq("username", username),
                            eq("tournament", tournament),
                            eq("status", "compiled"))).first();

            if (thisPlayer != null)
                users.add(username);
        }

        return users;
    }

    public GamePlayer createGamePlayer(String username) throws Exception {
        int port = 9157;
        Document aPlayer =
                players.find(and(
                        eq("status", "compiled"),
                        eq("username", username))).sort(descending("createdAt")).first();
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
//            System.out.println("File name = " + f.getName());
            String playerName = f.getName().split("\\.(?=[^\\.]+$)")[0];
            String playerPackage = packageName + "." + playerName;
            Class aClass = cl.loadClass(playerPackage);

//            System.out.println("...........detecting gamer");
            // found one and update player name, status and path to classes.
            if (Gamer.class.isAssignableFrom(aClass)) {
//                System.out.println( f.getName()
//                    + " is a player!!, and it is " + aClass.getSimpleName());
                // Setup players
                Gamer gamer = (Gamer) aClass.newInstance();
                GamePlayer aGamePlayer = new GamePlayer(port, gamer);
                return aGamePlayer;
            }
        }
        return null;
    }

    private Document latestMatch(String tournament) {
        return matches.find(eq("tournament", tournament)).sort(descending("createdAt")).first();
    }

    private Map<IPlayer, Rating> updateOneVsOneRating(Match match, Rating p1Rating, Rating p2Rating) {
        GameInfo gameInfo = GameInfo.getDefaultGameInfo();

        Player<String> player1 = new Player<String>(match.getPlayerNamesFromHost().get(0));
        Player<String> player2 = new Player<String>(match.getPlayerNamesFromHost().get(1));
        Team team1 = new Team(player1, p1Rating);
        Team team2 = new Team(player2, p2Rating);
        Collection<ITeam> teams = Team.concat(team1, team2);

        Map<IPlayer, Rating> newRatings;
        if (match.getGoalValues().get(0) > match.getGoalValues().get(1))
            newRatings = twoPlayersCalculator.calculateNewRatings(gameInfo, teams, 1, 2);
        else if (match.getGoalValues().get(0) < match.getGoalValues().get(1))
            newRatings = twoPlayersCalculator.calculateNewRatings(gameInfo, teams, 2, 1);
        else
            newRatings = twoPlayersCalculator.calculateNewRatings(gameInfo, teams, 1, 1);
        return newRatings;
    }

    private void updateFirstOneVsOneMatch(Match match) {
        // match result
        List<Document> matchResult = new ArrayList<>();
        for (int i = 0; i < match.getGoalValues().size(); i++) {
            matchResult.add(new Document("username", match.getPlayerNamesFromHost().get(i))
                    .append("score", match.getGoalValues().get(i)));
        }

        // init ranks by default rating, mu and
        GameInfo gameInfo = GameInfo.getDefaultGameInfo();
        Map<String, Document> userRankMap = new HashMap<>();
        for (String user : usersInTournament()) {
            userRankMap.put(user, new Document("username", user)
                    .append("rating", gameInfo.getDefaultRating().getConservativeRating())
                    .append("mu", gameInfo.getDefaultRating().getMean())
                    .append("sigma", gameInfo.getDefaultRating().getStandardDeviation())
                    .append("numMatch", 0));
        }

        // update new rating from match result
        Double mu = gameInfo.getDefaultRating().getMean();
        Double sigma = gameInfo.getDefaultRating().getStandardDeviation();
        Map<IPlayer, Rating> newRatings =
                updateOneVsOneRating(match, new Rating(mu, sigma), new Rating(mu, sigma));

        for (Map.Entry<IPlayer, Rating> entry : newRatings.entrySet()) {
            String username = entry.getKey().toString();
            Rating newRating = entry.getValue();
            int numMatch = userRankMap.get(username).getInteger("numMatch") + 1;
            userRankMap.put(entry.getKey().toString(),new Document("username", username)
                    .append("rating", newRating.getConservativeRating())
                    .append("mu", newRating.getMean())
                    .append("sigma", newRating.getStandardDeviation())
                    .append("numMatch", Integer.valueOf(numMatch)));
        }

        List<Document> ranks = new ArrayList<Document>(userRankMap.values());
        insertNewMatch(tournament, match, matchResult, ranks);
    }

    private void updateOneVsOneMatch(Match match) {
        Document latestMatch = latestMatch(tournament);
        if (latestMatch == null) {
            updateFirstOneVsOneMatch(match);
            return;
        }

        // init userRankMap from latest ranks
        Map<String, Document> userRankMap = new HashMap<>();
        List<Document> latestRanks = (List<Document>)latestMatch.get("ranks");
        for (Document rank : latestRanks) {
            userRankMap.put(rank.getString("username"), rank);
        }

        // init default values for users not in latest ranks
        GameInfo gameInfo = GameInfo.getDefaultGameInfo();
        for (String username : match.getPlayerNamesFromHost()) {
            if (! userRankMap.containsKey(username)) {
                Document newUserRank = new Document("username", username)
                        .append("rating", gameInfo.getDefaultRating().getConservativeRating())
                        .append("mu", gameInfo.getDefaultRating().getMean())
                        .append("sigma", gameInfo.getDefaultRating().getStandardDeviation())
                        .append("numMatch", 0);

                userRankMap.put(username, newUserRank);
            }
        }

        // update ratings
        String user1 = match.getPlayerNamesFromHost().get(0);
        String user2 = match.getPlayerNamesFromHost().get(1);
        Rating p1Rating = new Rating(userRankMap.get(user1).getDouble("mu"), userRankMap.get(user1).getDouble("sigma"));
        Rating p2Rating = new Rating(userRankMap.get(user2).getDouble("mu"), userRankMap.get(user2).getDouble("sigma"));
        Map<IPlayer, Rating> newRatings = updateOneVsOneRating(match, p1Rating, p2Rating);

        for (Map.Entry<IPlayer, Rating> entry : newRatings.entrySet()) {
            String username = entry.getKey().toString();
            Rating newRating = entry.getValue();

            if (userRankMap.containsKey(username)) {
                int numMatch = userRankMap.get(username).getInteger("numMatch") + 1;
                userRankMap.put(entry.getKey().toString(), new Document("username", username)
                        .append("rating", newRating.getConservativeRating())
                        .append("mu", newRating.getMean())
                        .append("sigma", newRating.getStandardDeviation())
                        .append("numMatch", numMatch));
            }
        }

        List<Document> ranks = new ArrayList<Document>(userRankMap.values());

        // match result
        List<Document> matchResult = new ArrayList<>();
        for (int i = 0; i < match.getGoalValues().size(); i++)
            matchResult.add(new Document("username", match.getPlayerNamesFromHost().get(i))
                    .append("score", match.getGoalValues().get(i)));

        insertNewMatch(tournament, match, matchResult, ranks);
    }

    private void insertNewMatch(
            String tournamentName, Match match, List<Document> matchResult, List<Document> ranks) {

        String tour_id = tournaments.find(eq("name", tournamentName)).first().getString("_id");
        Document thisMatch =
                new Document("tournament", tournament)
                        .append("tournament_id", tour_id)
                        .append("match_id", match.getMatchId())
                        .append("result", matchResult)
                        .append("ranks", ranks)
                        .append("createdAt", new Date());
        matches.insertOne(thisMatch);
    }

    private void saveMatch(Match match) throws IOException {
        System.out.println("saveMatch");
        // create dir to store game results in JSON
        File f = new File(tournament);
        if (!f.exists()) {
            f.mkdir();
            // f = new File(tournament + "/scores");
            f.createNewFile();
        }

        // // Open up the JSON file for this match, and save the match there.
        f = new File(tournament + "/" + match.getMatchId() + ".json");
        if (f.exists()) f.delete();

        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(match.toJSON());
        bw.flush();
        bw.close();
    }

    @Override
    public void observe(Event genericEvent) {
        if (!(genericEvent instanceof ServerMatchUpdatedEvent)) return;
        ServerMatchUpdatedEvent event = (ServerMatchUpdatedEvent)genericEvent;
        Match match = event.getMatch();

        if (match.isCompleted()) {
            System.out.println("............. Match is completed.");
            // shut down players
            for (GamePlayer aPlayer : playerMap.get(match.getMatchId()))
                aPlayer.shutdown();

            // make users available
            availableUsers.addAll(match.getPlayerNamesFromHost());

            // update DB
            List<String> usernames = match.getPlayerNamesFromHost();
            // if a tournament hasn't started, set player's skills to default

            GAME_TYPE gameType = GAME_TYPE.ONE_VS_ONE;
            switch (gameType) {
                case SINGLE: break;
                case ONE_VS_ONE: updateOneVsOneMatch(match);
            }

            try {
                saveMatch(match);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return;
        }

        // System.out.println("play clock ---> " + event.getMatch().getPlayClock());
        // System.out.println("getMostRecentMoves ---> " + event.getMatch().getMostRecentMoves());
        return;
    }

    public void playGame(List<String> hostNames,
                         List<String> playerNames,
                         List<Integer> portNumbers,
                         List<GamePlayer> gamePlayers,
                         String gameKey) throws IOException, InterruptedException {

        Game game = GameRepository.getDefaultRepository().getGame(gameKey);
        int expectedRoles = Role.computeRoles(game.getRules()).size();
        if (hostNames.size() != expectedRoles) {
            throw new RuntimeException("Invalid number of players for game " + gameKey + ": " + hostNames.size() + " vs " + expectedRoles);
        }

        String matchId = tournament + "." + gameKey + "." + System.currentTimeMillis();
        int startClock = 3;
        int playClock = 14;
        Match match = new Match(matchId, -1, startClock, playClock, game);
        match.setPlayerNamesFromHost(playerNames);

        // run players
        for (GamePlayer aGamePlayer : gamePlayers) {
            aGamePlayer.start();
        }

        // update players and match status
        playerMap.put(matchId, gamePlayers);
        availableUsers.removeAll(playerNames);

        // run the match
        GameServer server = new GameServer(match, hostNames, portNumbers);
        server.addObserver(this);
        server.start();
        // server.join();
    }

    public void playOneVsOne(List<String> pickedUsers) throws Exception {
        List<String> hostNames = new ArrayList<String>();
        List<Integer> portNumbers = new ArrayList<>();
        List<String> playerNames = new ArrayList<String>();
        List<GamePlayer> gamePlayers = new ArrayList<GamePlayer>();

        for (String username : pickedUsers) {
            hostNames.add("localhost");
            GamePlayer aPlayer = createGamePlayer(username);
            gamePlayers.add(aPlayer);
            portNumbers.add(aPlayer.getGamerPort());
            playerNames.add(username);
        }

        // playGame needs ranks(1,2,3) as a return object.
        playGame(hostNames, playerNames, portNumbers, gamePlayers, gameKey);
    }

}