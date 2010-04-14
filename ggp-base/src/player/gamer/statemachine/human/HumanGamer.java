package player.gamer.statemachine.human;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.human.event.HumanNewMovesEvent;
import player.gamer.statemachine.human.event.HumanTimeoutEvent;
import player.gamer.statemachine.human.gui.HumanDetailPanel;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import apps.player.detail.DetailPanel;

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
}