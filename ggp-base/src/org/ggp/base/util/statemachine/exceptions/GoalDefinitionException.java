package org.ggp.base.util.statemachine.exceptions;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

@SuppressWarnings("serial")
public final class GoalDefinitionException extends Exception
{

	private final Role role;
	private final MachineState state;

	public GoalDefinitionException(MachineState state, Role role)
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
		return "Goal is poorly defined for " + role + " in " + state;
	}

}
