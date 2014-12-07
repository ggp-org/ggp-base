package org.ggp.base.apps.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.statemachine.random.RandomGamer;
import org.ggp.base.server.GameServer;
import org.ggp.base.server.event.ServerNewMovesEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.presence.PlayerPresenceManager.InvalidHostportException;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.Symbol;
import org.ggp.base.util.symbol.grammar.SymbolList;

/**
 * PlayerTester is a benchmarking tool to evaluate the skill of a player.
 * It only requires one player to be running and uses only minimal extra
 * memory, so it can run on the same machine as the player. It run a set
 * of test cases, and computes a benchmark score for the player based on
 * how many of the tests the player passes.
 *
 * Tests consist of a game, a state in that game, a role that the player
 * will act as, and a set of "good" move choices. The player passes the
 * test if, when put into a match of the given game in the given state as
 * the given role, the player chooses one of the "good" move choices in a
 * reasonable amount of time.
 *
 * Mechanically, this is done by constructing an artificial game in which
 * the original game is modified to start in the chosen state, by adjusting
 * the set of "init" propositions. The player is put into a match playing
 * the game, with a suitable start clock, and runs through exactly one move.
 * Then the move is checked against the list of "good" moves, a "pass/fail"
 * verdict is issued, and the match is aborted. Repeat for all tests.
 *
 * The benchmark score is deterministic if the player is deterministic.
 *
 * @author Sam Schreiber
 */
public class PlayerTester {
    /**
     * getMediasResGame takes a game and a desired initial state, and rewrites
     * the game rules so that the game begins in that initial state. To do this
     * it removes all of the "init" rules and substitutes in its own set.
     * @throws SymbolFormatException
     */
    private static Game getMediasResGame(String gameKey, String theState) throws SymbolFormatException {
    	StringBuilder newRulesheet = new StringBuilder();
    	List<Gdl> theRules = GameRepository.getDefaultRepository().getGame(gameKey).getRules();
    	for (Gdl gdl : theRules) {
    		if (gdl instanceof GdlRule) {
    			// Capture and drop init statements of the form "( <= ( init ( pool ?piece ) ) ( piece ?piece ) )"
    			GdlRule rule = (GdlRule)gdl;
    			if (rule.getHead().getName().equals(GdlPool.INIT)) {
    				;
    			} else {
    				newRulesheet.append(gdl.toString() + "\n");
    			}
    		} else if (gdl instanceof GdlRelation) {
    			// Capture and drop init statements of the form "( init ( cell 4 4 empty ) )"
    			GdlRelation rel = (GdlRelation)gdl;
    			if (rel.getName().equals(GdlPool.INIT)) {
    				;
    			} else {
    				newRulesheet.append(gdl.toString() + "\n");
    			}
    		} else {
    			newRulesheet.append(gdl.toString() + "\n");
    		}
    	}

    	// Once all of the existing init lines have been removed, add new ones.
		SymbolList newInitialState = (SymbolList)SymbolFactory.create(theState);
		for (int i = 0; i < newInitialState.size(); i++) {
			Symbol anInit = newInitialState.get(i);
			newRulesheet.append("( INIT " + anInit + " )\n");
		}

    	return Game.createEphemeralGame("( " + newRulesheet.toString() + " )");
    }

    public static boolean passesTest(String hostport, String gameKey, int nRole, String theState, String[] goodMovesArr) throws SymbolFormatException, InvalidHostportException {
    	final Game theGame = getMediasResGame(gameKey, theState);
    	final Match theMatch = new Match("playerTester." + Match.getRandomString(5), -1, 5, 5, theGame);

        // Set up fake players to pretend to play the game alongside the real player
        List<String> theHosts = new ArrayList<String>();
        List<Integer> thePorts = new ArrayList<Integer>();
        for (int i = 0; i < Role.computeRoles(theGame.getRules()).size(); i++) {
        	theHosts.add("SamplePlayer" + i);
        	thePorts.add(9147+i);
        }
        theHosts.set(nRole, hostport.split(":")[0]);
        thePorts.set(nRole, Integer.parseInt(hostport.split(":")[1]));

        // Set up a game server to play through the game, with all players playing randomly
        // except the real player that we're testing
        final GameServer theServer = new GameServer(theMatch, theHosts, thePorts);
        for (int i = 0; i < theHosts.size(); i++) {
        	if (i != nRole) {
        		theServer.makePlayerPlayRandomly(i);
        	}
        }

        final int theRole = nRole;
        final Move[] theChosenMoveArr = new Move[1];
        theServer.addObserver(new Observer() {
			@Override
			public void observe(Event event) {
				if (event instanceof ServerNewMovesEvent) {
					theChosenMoveArr[0] = ((ServerNewMovesEvent)event).getMoves().get(theRole);
					theServer.abort();
				}
			}
		});

    	theServer.start();
        try {
			theServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        final String theChosenMove = theChosenMoveArr[0].toString().toLowerCase();
        Set<String> goodMoves = new HashSet<String>(Arrays.asList(goodMovesArr));
        boolean passes = goodMoves.contains(theChosenMove);

        if (!passes) {
        	System.out.println("Failure: player chose " + theChosenMove + "; acceptable moves are " + goodMoves);
        } else {
        	System.out.println("Success: player chose " + theChosenMove + " which is an acceptable move.");
        }

        return passes;
    }

    public static void main(String[] args) throws InterruptedException, SymbolFormatException, IOException, InvalidHostportException {
    	GamePlayer player = new GamePlayer(3141, new RandomGamer());
    	player.start();
   		passesTest("127.0.0.1:3141", "ticTacToe", 0, "( ( cell 2 1 b ) ( control xplayer ) ( cell 1 3 b ) ( cell 1 2 b ) ( cell 2 3 b ) ( cell 3 3 b ) ( cell 3 1 b ) ( cell 2 2 b ) ( cell 1 1 b ) ( cell 3 2 b ) )", new String[] { "( mark 2 2 )" });
   		player.shutdown();
    }
}