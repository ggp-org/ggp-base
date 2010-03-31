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

	public void setMove(Move move)
	{
		this.move = move;
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Do nothing.
	}

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