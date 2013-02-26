package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Arrays;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SamplePuzzleGamer is a sample gamer that's specifically designed for solving
 * puzzles by searching for moves which can eventually lead to high-value goals.
 * It cannot handle games which have opponents, since it assumes that all of the
 * moves in the game are being made by itself.
 */
public final class SamplePuzzleGamer extends SampleGamer
{
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		// SamplePuzzleGamer can only play single player games (puzzles).
		if (getStateMachine().getRoles().size() > 1) {
			return new Move(GdlPool.getConstant("OOPS"));
		}
		
		// From the current state, consider all of the legal moves, and the states that
		// they will move the game into, and of those states, pick the one in which we can
		// get the highest score.
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;
		
		for (Move move : moves) {
			MachineState stateAfterMove = getStateMachine().getNextState(getCurrentState(), Arrays.asList(new Move[]{move}));
			int bestScoreAfterMove = getBestScore(stateAfterMove);
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
	
	private int getBestScore(MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, getRole());
		}
		
		int bestScoreSoFar = -1;
		
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for (Move move : moves) {
			MachineState gameStateAfterMove = getStateMachine().getNextState(state, Arrays.asList(new Move[]{move}));
			int bestScoreAfterMove = getBestScore(gameStateAfterMove);
			bestScoreSoFar = Math.max(bestScoreSoFar, bestScoreAfterMove);
			if (bestScoreSoFar == 100) break;
		}
		
		return bestScoreSoFar;
	}	
}