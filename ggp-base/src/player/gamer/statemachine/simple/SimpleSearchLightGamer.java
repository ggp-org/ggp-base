package player.gamer.statemachine.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import apps.player.detail.DetailPanel;

/**
 * SimpleSearchLightGamer is a simple state-machine-based Gamer. It will,
 * to the best of its ability, never pick a move which will give its opponent
 * a possible one-move win. It will also spend up to two seconds looking for
 * one-move wins it can take. This makes it slightly more challenging than the
 * RandomGamer, while still playing reasonably fast.
 * 
 * Essentially, it has a one-move search-light that it shines out, allowing it
 * to avoid moves that are immediately terrible, and also choose moves that are
 * immediately excellent.
 * 
 * This approach implicitly assumes that it is playing an alternating-play game,
 * which is not always true. It will play simultaneous-action games less well.
 * It also assumes that it is playing a zero-sum game, where its opponent will
 * always force it to lose if given that option.
 * 
 * This player is fairly good at games like Tic-Tac-Toe and Connect Four.
 * This player is pretty terrible at most games.
 * 
 * @author Sam Schreiber
 */
public final class SimpleSearchLightGamer extends StateMachineGamer
{
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Do nothing.
	}

	private Random theRandom = new Random();
	
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;
		
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = (moves.get(new Random().nextInt(moves.size())));
		
		// Shuffle the moves into a random order, so that when we find the first
		// move that doesn't give our opponent a forced win, we aren't always choosing
		// the first legal move over and over (which is visibly repetitive).
		List<Move> movesInRandomOrder = new ArrayList<Move>();
		while(!moves.isEmpty()) {
		    Move aMove = moves.get(theRandom.nextInt(moves.size()));
		    movesInRandomOrder.add(aMove);
		    moves.remove(aMove);
		}
		
		// Go through all of the legal moves in a random over, and consider each one.
		// For each move, we want to determine whether taking that move will give our
		// opponent a one-move win. We're also interested in whether taking that move
		// will immediately cause us to win or lose.
		//
		// Our main goal is to find a move which won't give our opponent a one-move win.
		// We will also continue considering moves for two seconds, in case we can stumble
		// upon a move which would cause us to win: if we find such a move, we will just
		// immediately take it.
		boolean reasonableMoveFound = false;
		for(Move moveUnderConsideration : movesInRandomOrder) {
		    // Check to see if there's time to continue.
		    if(System.currentTimeMillis() > finishBy) break;
		    
		    // If we've found a reasonable move, only spend at most two seconds trying
		    // to find a winning move.
		    if(System.currentTimeMillis() > start + 2000 && reasonableMoveFound) break;
		    
		    // Get the next state of the game, if we take the move we're considering.
		    // Since it's our turn, in an alternating-play game the opponent will only
		    // have one legal move, and so calling "getRandomJointMove" with our move
		    // fixed will always return the joint move consisting of our move and the
		    // opponent's no-op. In a simultaneous-action game, however, the opponent
		    // may have many moves, and so we will randomly pick one of our opponent's
		    // possible actions and assume they do that.
		    MachineState nextState = theMachine.getNextState(getCurrentState(), theMachine.getRandomJointMove(getCurrentState(), getRole(), moveUnderConsideration));
		    
		    // Does the move under consideration end the game? If it does, do we win
		    // or lose? If we lose, don't bother considering it. If we win, then we
		    // definitely want to take this move.
		    if(theMachine.isTerminal(nextState)) {
		        if(theMachine.getGoal(nextState, getRole()) == 0) {
		            continue;
		        } else if(theMachine.getGoal(nextState, getRole()) == 100) {
	                selection = moveUnderConsideration;
	                break;
		        }
		    }
		    
		    // Check whether any of the legal joint moves from this state lead to
		    // a loss for us. Again, this only makes sense in the context of an alternating
		    // play zero-sum game, in which this is the opponent's move and they are trying
		    // to make us lose, and so if they are offered any move that will make us lose
		    // they will take it.
		    boolean forcedLoss = false;		    		    
		    for(List<Move> jointMove : theMachine.getLegalJointMoves(nextState)) {
		        MachineState nextNextState = theMachine.getNextState(nextState, jointMove);
		        if(theMachine.isTerminal(nextNextState)) {
		            if(theMachine.getGoal(nextNextState, getRole()) == 0) {
		                forcedLoss = true;
		                break;
		            }
		        }
		        
		        // Check to see if there's time to continue.
		        if(System.currentTimeMillis() > finishBy) {
		            forcedLoss = true;
		            break;
		        }
		    }
		    
		    // If we've verified that this move isn't going to lead us to a state where
		    // our opponent can defeat us in one move, we should keep track of it.
		    if(!forcedLoss) {
		        selection = moveUnderConsideration;
		        reasonableMoveFound = true;
		    }
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedProverStateMachine();
	}

	@Override
	public String getName() {
		return "SimpleSearchLight";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}