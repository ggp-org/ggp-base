package util.gdl.transforms;

import java.io.File;
import java.util.List;

import util.configuration.LocalResourceLoader;
import util.configuration.ProjectConfiguration;
import util.gdl.grammar.Gdl;
import util.statemachine.implementation.prover.ProverStateMachine;
import util.statemachine.verifier.StateMachineVerifier;

/**
 * 
 * @author Sam Schreiber
 *
 */
public class TransformTester {
	public static void main(String args[]) {
	    
	    final boolean showDiffs = false;
        final ProverStateMachine theReference = new ProverStateMachine();
        final ProverStateMachine theMachine = new ProverStateMachine();	    
	    
        for(File game : ProjectConfiguration.gameRulesheetsDirectory.listFiles()) {
            if(!game.getName().endsWith(".kif")) continue;
            if(game.getName().contains("laikLee")) continue;
            System.out.println(game.getName());
            List<Gdl> description = LocalResourceLoader.loadGame(game.getName().replace(".kif", ""));
            List<Gdl> newDescription = description;
            
            //Choose the transformation(s) to test here
            description = DeORer.run(description);
            newDescription = VariableConstrainer.replaceFunctionValuedVariables(description);
            
            if(description.hashCode() != newDescription.hashCode()) {
                theReference.initialize(description);
                theMachine.initialize(newDescription);
                System.out.println("Detected activation in game " + game.getName() + ". Checking consistency: ");
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
