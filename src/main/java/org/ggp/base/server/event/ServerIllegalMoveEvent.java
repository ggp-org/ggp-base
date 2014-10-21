package org.ggp.base.server.event;

import java.io.Serializable;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;


@SuppressWarnings("serial")
public final class ServerIllegalMoveEvent extends Event implements Serializable
{

	private final Move move;
	private final Role role;

	public ServerIllegalMoveEvent(Role role, Move move)
	{
		this.role = role;
		this.move = move;
	}

	public Move getMove()
	{
		return move;
	}

	public Role getRole()
	{
		return role;
	}

}
