package org.ggp.base.player.gamer.statemachine.human;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.player.gamer.statemachine.human.event.HumanNewMovesEvent;
import org.ggp.base.player.gamer.statemachine.human.event.HumanTimeoutEvent;
import org.ggp.base.player.gamer.statemachine.human.gui.HumanDetailPanel;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.cache.CachedProverStateMachine;


public final class HumanGamer extends StateMachineGamer
{
	private Move move;
	
	/**
	 * Sets the currentMove
	 * @param move 
	 */
	public void setMove(Move move)
	{
		this.move = move;
	}
	/**
	 * Default constructor
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Do nothing.
	}
	
	/**
	 * Selects the default move as the first legal move, and then waits while the Human sets their move
	 */
	@Override
	public synchronized Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		move = moves.get(0);

		try
		{
			notifyObservers(new HumanNewMovesEvent(moves, move));
			wait(timeout - System.currentTimeMillis() - 500);
			notifyObservers(new HumanTimeoutEvent(this));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return move;
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
		return "Human";
	}
	
	
	@Override
	public DetailPanel getDetailPanel() {
		return new HumanDetailPanel();
	}
	
	@Override
	public boolean isComputerPlayer() {
		return false;
	}	
}