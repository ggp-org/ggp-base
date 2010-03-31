package util.statemachine.exceptions;

import util.statemachine.MachineState;
import util.statemachine.Role;

@SuppressWarnings("serial")
public final class MoveDefinitionException extends Exception
{

	private final Role role;
	private final MachineState state;

	public MoveDefinitionException(MachineState state, Role role)
	{
		this.state = state;
		this.role = role;
	}

	public Role getRole()
	{
		return role;
	}

	public MachineState getState()
	{
		return state;
	}

	@Override
	public String toString()
	{
		return "There are no legal moves defined for " + role + " in " + state;
	}

}
