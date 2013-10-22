package org.ggp.base.util.gdl.transforms;

import java.util.List;

import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.verifier.StateMachineVerifier;


/**
 * 
 * @author Sam Schreiber
 *
 */
public class TransformTester {
	public static void main(String args[]) throws InterruptedException {
	    
	    final boolean showDiffs = false;
        final ProverStateMachine theReference = new ProverStateMachine();
        final ProverStateMachine theMachine = new ProverStateMachine();	    
	    
        GameRepository theRepository = GameRepository.getDefaultRepository();
        for(String gameKey : theRepository.getGameKeys()) {
            if(gameKey.contains("laikLee")) continue;
            List<Gdl> description = theRepository.getGame(gameKey).getRules();
            List<Gdl> newDescription = description;
            
            // Choose the transformation(s) to test here
            description = DeORer.run(description);
            newDescription = VariableConstrainer.replaceFunctionValuedVariables(description);
            
            if(description.hashCode() != newDescription.hashCode()) {
                theReference.initialize(description);
                theMachine.initialize(newDescription);
                System.out.println("Detected activation in game " + gameKey + ". Checking consistency: ");
                StateMachineVerifier.checkMachineConsistency(theReference, theMachine, 10000);
                
                if(showDiffs) {
                    for(Gdl x : newDescription) {
                        if(!description.contains(x))
                            System.out.println("NEW: " + x);
                    }
                    for(Gdl x : description) {
                        if(!newDescription.contains(x))
                            System.out.println("OLD: " + x);
                    }
                }
            }
        }	    
	}	
}
