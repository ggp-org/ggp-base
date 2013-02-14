package org.ggp.base.player.gamer.statemachine.reflex.legal;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.player.gamer.exception.GameAnalysisException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import org.ggp.base.player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.cache.CachedProverStateMachine;


/**
 * LegalGamer is a very simple state-machine-based Gamer that will always
 * pick the first legal move that it finds at any state in the game. This
 * is one of a family of simple "reflex" gamers which act entirely on reflex
 * (picking the first legal move, or a random move) regardless of the current
 * state of the game.
 * 
 * This is not really a serious approach to playing games, and is included in
 * this package merely as an example of a functioning Gamer.
 */
public final class LegalGamer extends StateMachineGamer
{
	
	/**
	 * Does nothing for the metagame
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Do nothing.
	}
	
	/**
	 * Selects the first legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);

		long stop = System.currentTimeMillis();

		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		return selection;
	}
	
	@Override
	public void stateMachineStop() {
		// Do nothing.
	}

	/**
	 * Uses a CachedProverStateMachine
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedProverStateMachine();
	}
	@Override
	public String getName() {
		return "Legal";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}

	@Override
	public void analyze(Game g, long timeout) throws GameAnalysisException {
		;
	}
}
