package apps.utilities;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import external.JSON.JSONException;
import external.JSON.JSONObject;

import util.crypto.SignableJSON;
import util.crypto.BaseCryptography.EncodedKeyPair;
import util.files.FileUtils;
import util.game.Game;
import util.game.GameRepository;
import util.gdl.grammar.GdlSentence;
import util.match.Match;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import util.statemachine.verifier.StateMachineVerifier;

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
        Match theMatch = new Match("simpleGameSim." + Match.getRandomString(5), 0, 0, theGame);
        try {
            theMatch.setCryptographicKeys(new EncodedKeyPair(FileUtils.readFileAsString(new File("src/apps/utilities/SimpleGameSimKeys.json"))));
        } catch (JSONException e) {
            System.err.println("Could not load cryptograhic keys: " + e);
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