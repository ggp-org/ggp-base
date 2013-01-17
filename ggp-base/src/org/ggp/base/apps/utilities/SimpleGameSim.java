package org.ggp.base.apps.utilities;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.crypto.SignableJSON;
import org.ggp.base.util.crypto.BaseCryptography.EncodedKeyPair;
import org.ggp.base.util.files.FileUtils;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import org.ggp.base.util.statemachine.verifier.StateMachineVerifier;

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
    
    public static void main(String[] args) {
        Game theGame = GameRepository.getDefaultRepository().getGame("nineBoardTicTacToe");
        Match theMatch = new Match("simpleGameSim." + Match.getRandomString(5), -1, 0, 0, theGame);
        theMatch.setPlayerNamesFromHost(Arrays.asList(new String[] {"SamplePlayer1", "SamplePlayer2"}));
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
        
        // ---------------------------------------------------------
        // Construct the machine: change this to select which machine
        // you're interested in using to simulate the game.
        StateMachine theMachine = new CachedProverStateMachine();        
        theMachine.initialize(theGame.getRules());
        
        // Check for consistency, before we simulate the game.
        StateMachine theProver = new ProverStateMachine();
        theProver.initialize(theGame.getRules());
        if(!StateMachineVerifier.checkMachineConsistency(theProver, theMachine, 1000)) {
            System.err.println("Inconsistency!");
        }

        // Go through and simulate one play through the game,
        // displaying the state as we go.
        int nState = 0;        
        try {
            System.out.println();
            Set<GdlSentence> oldContents = new HashSet<GdlSentence>();
            MachineState theCurrentState = theMachine.getInitialState();            
            do {
                theMatch.appendState(theCurrentState.getContents());
                if(nState > 0) System.out.print("State[" + nState + "]: ");
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
                oldContents = newContents;
                
                if(showCurrentState) System.out.println("State["+nState+"] Full: " + theCurrentState);
                List<Move> theJointMove = theMachine.getRandomJointMove(theCurrentState);
                System.out.println("Move taken: " + theJointMove);
                theCurrentState = theMachine.getNextStateDestructively(theCurrentState, theJointMove);
                theMatch.appendMoves2(theJointMove);
                theMatch.appendNoErrors();
                nState++;
            } while(!theMachine.isTerminal(theCurrentState));
            theMatch.appendState(theCurrentState.getContents());
            theMatch.markCompleted(theMachine.getGoals(theCurrentState));

            // Game over! Display goals.
            System.out.println("State["+nState+"] Full (Terminal): " + theCurrentState);
            System.out.println("Match information: " + theMatch);            
            for(Role r : theMachine.getRoles())
                System.out.println("Goal for " + r + ": " + theMachine.getGoal(theCurrentState, r));
            System.out.println("Match information cryptographically signed? " + SignableJSON.isSignedJSON(new JSONObject(theMatch.toJSON())));
            System.out.println("Match information cryptographic signature valid? " + SignableJSON.verifySignedJSON(new JSONObject(theMatch.toJSON())));
            System.out.println("Game over.");            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }    
}