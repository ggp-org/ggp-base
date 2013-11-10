package org.ggp.base.player.gamer.statemachine.strategic;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.player.strategy.algorithm.SearchAlgorithm;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class StrategicGamer extends SampleGamer {

	private SearchAlgorithm searchAlgorithm;

	/**
	 * Employs a search algorithm to select the best available move for the
	 * player within the alloted time.
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// given
		StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishByMillis = timeout - 1000;
		// when
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = searchAlgorithm.getBestMove(moves, finishByMillis);
		// then
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));

		return selection;
	}

	public void setSearchAlgorithm(SearchAlgorithm searchAlgorithm) {
		this.searchAlgorithm = searchAlgorithm;
	}

}