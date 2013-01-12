package org.ggp.base.validator.exception;

import org.ggp.base.util.statemachine.Role;

@SuppressWarnings("serial")
public final class MonotonicityException extends Exception
{

	private final Role role;

	public MonotonicityException(Role role)
	{
		this.role = role;
	}

	public Role getRole()
	{
		return role;
	}

	@Override
	public String toString()
	{
		return "Non-monotonic goal detected for role: " + role + "!";
	}

}
