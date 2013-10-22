package org.ggp.base.apps.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.server.GameServer;
import org.ggp.base.server.event.ServerCompletedMatchEvent;
import org.ggp.base.server.event.ServerNewGameStateEvent;
import org.ggp.base.server.event.ServerNewMovesEvent;
import org.ggp.base.util.crypto.SignableJSON;
import org.ggp.base.util.crypto.BaseCryptography.EncodedKeyPair;
import org.ggp.base.util.files.FileUtils;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * SimpleGameSim is a utility program that lets you simulate play through a
 * given game, and see which propositions become true/false in each state as
 * the game is played. This can be used to understand how a game runs, or to
 * debug issues in the state machine implementation.
 * 
 * It can also hide the step counter / control proposition, though this is
 * done in a very naive way, by just looking for (step ?x) and (control ?x)
 * propositions. None the less, it's still useful to have.
 * 
 * @author Sam Schreiber
 */
public class SimpleGameSim {
    public static final boolean hideStepCounter = true;
    public static final boolean hideControlProposition = true;
    public static final boolean showCurrentState = false;
    
    public static void main(String[] args) throws InterruptedException {
        final Game theGame = GameRepository.getDefaultRepository().getGame("nineBoardTicTacToe");
        final Match theMatch = new Match("simpleGameSim." + Match.getRandomString(5), -1, 0, 0, theGame);
        try {
            // Load a sample set of cryptographic keys. These sample keys are not secure,
            // since they're checked into the public GGP Base SVN repository. They are provided
            // merely to illustrate how the crypto key API in Match works. If you want to prove
            // that you ran a match, you need to generate your own pair of cryptographic keys,
            // keep them secure and hidden, and pass them to "setCryptographicKeys" in Match.
            // The match will then be signed using those keys. Do not use the sample keys if you
            // want to actually prove anything.
            theMatch.setCryptographicKeys(new EncodedKeyPair(FileUtils.readFileAsString(new File("src/org/ggp/base/apps/utilities/SampleKeys.json"))));
        } catch (JSONException e) {
            System.err.println("Could not load sample cryptograhic keys: " + e);
        }
        
        // Set up fake players to pretend to play the game
        List<String> fakeHosts = new ArrayList<String>();
        List<Integer> fakePorts = new ArrayList<Integer>();
        for (int i = 0; i < Role.computeRoles(theGame.getRules()).size(); i++) {
        	fakeHosts.add("SamplePlayer" + i);
        	fakePorts.add(9147+i);
        }

        // Set up a game server to play through the game, with all players playing randomly.
        final GameServer theServer = new GameServer(theMatch, fakeHosts, fakePorts);
        for (int i = 0; i < fakeHosts.size(); i++) {
        	theServer.makePlayerPlayRandomly(i);
        }
        
        // TODO: Allow a custom state machine to be plugged into the GameServer so that we can
        // simulate games using this tool with custom state machines, to verify they're sane.        
        
        final Set<GdlSentence> oldContents = new HashSet<GdlSentence>();
        final int[] nState = new int[1];
        theServer.addObserver(new Observer() {
			@Override
			public void observe(Event event) {
				if (event instanceof ServerNewGameStateEvent) {
					MachineState theCurrentState = ((ServerNewGameStateEvent)event).getState();
	                if(nState[0] > 0) System.out.print("State[" + nState[0] + "]: ");
	                Set<GdlSentence> newContents = theCurrentState.getContents();
	                for(GdlSentence newSentence : newContents) {
	                    if(hideStepCounter && newSentence.toString().contains("step")) continue;
	                    if(hideControlProposition && newSentence.toString().contains("control")) continue;
	                    if(!oldContents.contains(newSentence)) {
	                        System.out.print("+" + newSentence + ", ");
	                    }
	                }
	                for(GdlSentence oldSentence : oldContents) {
	                    if(hideStepCounter && oldSentence.toString().contains("step")) continue;
	                    if(hideControlProposition && oldSentence.toString().contains("control")) continue;                    
	                    if(!newContents.contains(oldSentence)) {
	                        System.out.print("-" + oldSentence + ", ");
	                    }
	                }
	                System.out.println();
	                oldContents.clear();
	                oldContents.addAll(newContents);
	                
	                if(showCurrentState) System.out.println("State[" + nState[0] + "] Full: " + theCurrentState);
	                nState[0]++;
				} else if (event instanceof ServerNewMovesEvent) {
					System.out.println("Move taken: " + ((ServerNewMovesEvent)event).getMoves());
				} else if (event instanceof ServerCompletedMatchEvent) {
			        System.out.println("State[" + nState[0] + "] Full (Terminal): " + oldContents);
			        System.out.println("Match information: " + theMatch);
			        System.out.println("Goals: " + ((ServerCompletedMatchEvent)event).getGoals());
			        try {
			        	System.out.println("Match information cryptographically signed? " + SignableJSON.isSignedJSON(new JSONObject(theMatch.toJSON())));
			        	System.out.println("Match information cryptographic signature valid? " + SignableJSON.verifySignedJSON(new JSONObject(theMatch.toJSON())));
			        } catch (JSONException je) {
			        	je.printStackTrace();
			        }
			        System.out.println("Game over.");
				}
			}
		});
        
        theServer.start();
        theServer.join();
    }    
}