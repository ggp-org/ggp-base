package server.event;

import java.io.Serializable;

import util.observer.Event;
import util.statemachine.Role;

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
