package org.ggp.base.server.event;

import java.io.Serializable;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Role;


@SuppressWarnings("serial")
public final class ServerTimeoutEvent extends Event implements Serializable
{

	private final Role role;

	public ServerTimeoutEvent(Role role)
	{
		this.role = role;
	}

	public Role getRole()
	{
		return role;
	}

}
