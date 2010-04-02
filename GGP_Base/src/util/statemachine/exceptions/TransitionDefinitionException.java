package util.statemachine.exceptions;

import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;

@SuppressWarnings("serial")
public final class TransitionDefinitionException extends Exception
{

	private final List<Move> moves;
	private final MachineState state;

	public TransitionDefinitionException(MachineState state, List<Move> moves)
	{
		this.state = state;
		this.moves = moves;
	}

	public List<Move> getMoves()
	{
		return moves;
	}

	public MachineState getState()
	{
		return state;
	}

	@Override
	public String toString()
	{
		return "Transition is poorly defined for " + moves + " in " + state;
	}

}
