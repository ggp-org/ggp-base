package org.ggp.base.player.gamer.statemachine.human;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.player.gamer.statemachine.human.event.HumanNewMovesEvent;
import org.ggp.base.player.gamer.statemachine.human.event.HumanTimeoutEvent;
import org.ggp.base.player.gamer.statemachine.human.gui.HumanDetailPanel;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * HumanGamer is a simple apparatus for letting a human control a player,
 * by manually choosing moves in the player's detail panel. This player will
 * not work without a human actually interacting with the detail panel. This
 * player has a very simplistic user interface; if you actually want to play
 * as a human, you're probably better off using the purpose-built Kiosk app.
 */
public final class HumanGamer extends StateMachineGamer
{
	@Override
	public String getName() {
		return "Human";
	}

	/**
	 * Selects the default move as the first legal move, and then waits
	 * while the Human sets their move. This is done via the HumanDetailPanel.
	 */
	@Override
	public synchronized Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		move = moves.get(0);

		try {
			notifyObservers(new HumanNewMovesEvent(moves, move));
			wait(timeout - System.currentTimeMillis() - 500);
			notifyObservers(new HumanTimeoutEvent(this));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return move;
	}

	private Move move;
	public void setMove(Move move) {
		this.move = move;
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new HumanDetailPanel();
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Human gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Human gamer does no metagaming at the beginning of the match.
	}

	@Override
	public void stateMachineStop() {
		// Human gamer does no special cleanup when the match ends normally.
	}

	@Override
	public void stateMachineAbort() {
		// Human gamer does no special cleanup when the match ends abruptly.
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public boolean isComputerPlayer() {
		return false;
	}
}