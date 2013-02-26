package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SampleHeuristicGamer is an improvement on the @SampleMinimaxGamer which uses
 * heuristics to approximate scores for non-terminal states once it reaches a fixed
 * depth, rather than trying to perform minimax all the way to the bottom of the
 * game tree, which can require prohibitive amounts of time on larger games.
 */
public final class SampleHeuristicGamer extends SampleGamer
{
	/**
	 * MAX_DEPTH defines how many layers of the game tree should be expanded by
	 * the minimax algorithm before non-terminal states are evaluated using the
	 * heuristic functions.
	 */
	private static final int MAX_DEPTH = 5;
	
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;
		
		for (Move move : moves) {
			int bestScoreAfterMove = heuristicScoreForMove(getCurrentState(), move, MAX_DEPTH);
			if (bestScoreAfterMove > bestScoreSoFar) {
				bestScoreSoFar = bestScoreAfterMove;
				bestMoveSoFar = move;
				if (bestScoreSoFar == 100) {
					break;
				}
			}			
		}
		
		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, bestMoveSoFar, stop - start));
		return bestMoveSoFar;
	}
	
	private int heuristicScoreForMove(MachineState state, Move myMove, int depth)  throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		int worstScoreSoFar = 100;
		for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state, getRole(), myMove)) {
			int bestScoreSoFar = -1;
			MachineState stateAfterMove = getStateMachine().getNextState(state, jointMove);
			if (getStateMachine().isTerminal(stateAfterMove)) {
				bestScoreSoFar = getStateMachine().getGoal(stateAfterMove, getRole());
			} else if (depth == 0) {				
				bestScoreSoFar = new MixtureHeuristic().evaluate(stateAfterMove);
			} else {
				// Choose the move for us in the next state which maximizes our score				
				List<Move> moves = getStateMachine().getLegalMoves(stateAfterMove, getRole());
				for (Move myNextMove : moves) {
					int bestScoreAfterMove = heuristicScoreForMove(stateAfterMove, myNextMove, depth-1);
					bestScoreSoFar = Math.max(bestScoreSoFar, bestScoreAfterMove);
					if (bestScoreSoFar == 100) break;
				}
			}
			// Choose the joint move for the opponents that minimizes our score
			worstScoreSoFar = Math.min(worstScoreSoFar, bestScoreSoFar);
			if (worstScoreSoFar == 0) break;
		}
		return worstScoreSoFar;
	}
	
	/**
	 * Heuristics are used to assign scores to non-terminal states. They are inherently
	 * approximations, based on things like having many pieces, having a good position
	 * on the board, having many available moves, having accumulated many points, having
	 * captured an opponent's pieces, having tie-breaking pieces, etc. Coming up with
	 * heuristics that can be used across a wide range of games, rather than just for one
	 * specific game, can be challenging. This interface defines heuristics as having an
	 * evaluate function, which takes a state and returns a number between 0 and 100
	 * representing an approximate score for the state.
	 */
	interface Heuristic {
		int evaluate(MachineState state) throws MoveDefinitionException;
	}
	
	/**
	 * MixtureHeuristic is a mixture of other heuristics. Each heuristic will likely
	 * have strong points and weak points, and so by mixing them together, the overall
	 * evaluation is hopefully more significant than any of its individual components.
	 */
	class MixtureHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return (int)(0.75 * new MobilityHeuristic().evaluate(state) +
					      0.25 * new FactsHeuristic().evaluate(state));
		}
	}

	/**
	 * MobilityHeuristic is a heuristic that measures the goodness of a particular state
	 * by how many moves you have, compared to the total number of available joint moves.
	 * If you have many moves and your opponent has few, that's considered good.
	 * If you have few moves and your opponent has many, that's considered bad.
	 */
	class MobilityHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return 100 * getStateMachine().getLegalMoves(state, getRole()).size() / getStateMachine().getLegalJointMoves(state).size();
		}
	}
	
	/**
	 * FactsHeuristic is a heuristic that measures the goodness of a particular state
	 * as decreasing for every new fact required to represent the state. The intuition
	 * is that maybe it's better to aim for simpler states. This may not actually be a
	 * good idea, but it does make a good example heuristic.
	 */
	class FactsHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return 100 / Math.max(state.getContents().size(), 1);
		}
	}
}