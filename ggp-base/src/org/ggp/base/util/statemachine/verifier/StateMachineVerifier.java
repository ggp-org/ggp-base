package org.ggp.base.util.statemachine.verifier;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;


public class StateMachineVerifier {
    public static boolean checkMachineConsistency(StateMachine theReference, StateMachine theSubject, long timeToSpend) {
        long startTime = System.currentTimeMillis();

        GamerLogger.log("StateMachine", "Performing automatic consistency testing on " + theSubject.getClass().getName() + " using " + theReference.getClass().getName() + " as a reference.");

        List<StateMachine> theMachines = new ArrayList<StateMachine>();
        theMachines.add(theReference);
        theMachines.add(theSubject);
        
        GamerLogger.emitToConsole("Consistency checking: [");
        int nRound = 0;
        while(true) {
            nRound++;
            
            GamerLogger.emitToConsole(".");
            MachineState[] theCurrentStates = new MachineState[theMachines.size()];
            for(int i = 0; i < theMachines.size(); i++) {
                try {
                    theCurrentStates[i] = theMachines.get(i).getInitialState();
                } catch(Exception e) {
                    GamerLogger.log("StateMachine", "Machine #" + i + " failed to generate an initial state!");
                    return false;                    
                }
            }

            while(!theMachines.get(0).isTerminal(theCurrentStates[0])) {
                if(System.currentTimeMillis() > startTime + timeToSpend)
                    break;                
                
                // Do per-state consistency checks
                for(int i = 1; i < theMachines.size(); i++) {
                    for(Role theRole : theMachines.get(0).getRoles()) {
                        try {
                            if(!(theMachines.get(i).getLegalMoves(theCurrentStates[i], theRole).size() == theMachines.get(0).getLegalMoves(theCurrentStates[0], theRole).size())) {
                                GamerLogger.log("StateMachine", "Inconsistency between machine #" + i + " and ProverStateMachine over state " + theCurrentStates[0] + " vs " + theCurrentStates[i].getContents());
                                GamerLogger.log("StateMachine", "Machine #" + 0 + " has move count = " + theMachines.get(0).getLegalMoves(theCurrentStates[0], theRole).size() + " for player " + theRole);
                                GamerLogger.log("StateMachine", "Machine #" + i + " has move count = " + theMachines.get(i).getLegalMoves(theCurrentStates[i], theRole).size() + " for player " + theRole);
                                return false;
                            }
                        } catch(Exception e) {
                            GamerLogger.logStackTrace("StateMachine", e);
                        }
                    }
                }

                try {
                    //Proceed on to the next state.
                    List<Move> theJointMove = theMachines.get(0).getRandomJointMove(theCurrentStates[0]);

                    for(int i = 0; i < theMachines.size(); i++) {
                        try {
                            theCurrentStates[i] = theMachines.get(i).getNextState(theCurrentStates[i], theJointMove);
                        } catch(Exception e) {
                            GamerLogger.logStackTrace("StateMachine", e);
                        }
                    }    
                } catch(Exception e) {
                    GamerLogger.logStackTrace("StateMachine", e);
                }
            }

            if(System.currentTimeMillis() > startTime + timeToSpend)
                break;
            
            // Do final consistency checks
            for(int i = 1; i < theMachines.size(); i++) {
                if(!theMachines.get(i).isTerminal(theCurrentStates[i])) {
                    GamerLogger.log("StateMachine", "Inconsistency between machine #" + i + " and ProverStateMachine over terminal-ness of state " + theCurrentStates[0] + " vs " + theCurrentStates[i]);
                    return false;
                }
                for(Role theRole : theMachines.get(0).getRoles()) {
                    try {
                        theMachines.get(0).getGoal(theCurrentStates[0], theRole);
                    } catch(Exception e) {
                        continue;
                    }

                    try {
                        if(theMachines.get(i).getGoal(theCurrentStates[i], theRole) != theMachines.get(0).getGoal(theCurrentStates[0], theRole)) {
                            GamerLogger.log("StateMachine", "Inconsistency between machine #" + i + " and ProverStateMachine over goal value for " + theRole + " of state " + theCurrentStates[0] + ": " + theMachines.get(i).getGoal(theCurrentStates[i], theRole) + " vs " + theMachines.get(0).getGoal(theCurrentStates[0], theRole));
                            return false;
                        }
                    } catch(Exception e) {
                        GamerLogger.log("StateMachine", "Inconsistency between machine #" + i + " and ProverStateMachine over goal-ness of state " + theCurrentStates[0] + " vs " + theCurrentStates[i]);
                        return false;                        
                    }
                }
            }
        }
        GamerLogger.emitToConsole("]\n");

        GamerLogger.log("StateMachine", "Completed automatic consistency testing on " + theSubject.getClass().getName() + ", w/ " + nRound + " rounds: all tests pass!");
        return true;
    }
}