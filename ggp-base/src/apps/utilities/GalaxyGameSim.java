package apps.utilities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.galaxy.GalaxyRepository;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import util.statemachine.verifier.StateMachineVerifier;

/**
 * GalaxyGameSim is a version of the SimpleGameSim that loads games from
 * a remote repository server, according to the GGP Galaxy protocol. This
 * is an early prototype, and will be expanded upon.
 * 
 * @author Sam Schreiber
 */
public class GalaxyGameSim {
    public static final boolean hideStepCounter = true;
    public static final boolean hideControlProposition = true;
    public static final boolean showCurrentState = false;
    
    public static void main(String[] args) {
        List<Gdl> description = GalaxyRepository.getGameRulesheetFromRepository("http://ggp-repository.appspot.com", "nineBoardTicTacToe");
        
        // ---------------------------------------------------------
        // Construct the machine: change this to select which machine
        // you're interested in using to simulate the game.
        StateMachine theMachine = new CachedProverStateMachine();        
        theMachine.initialize(description);
        
        // Check for consistency, before we simulate the game.
        StateMachine theProver = new ProverStateMachine();
        theProver.initialize(description);
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
                nState++;
            } while(!theMachine.isTerminal(theCurrentState));
            
            // Game over! Display goals.
            System.out.println("State["+nState+"] Full (Terminal): " + theCurrentState);
            for(Role r : theMachine.getRoles())
                System.out.println("Goal for " + r + ": " + theMachine.getGoal(theCurrentState, r));
            System.out.println("Game over.");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }    
}