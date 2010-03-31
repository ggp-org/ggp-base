package server.event;

import java.io.Serializable;
import java.util.List;

import util.observer.Event;
import util.statemachine.Role;

@SuppressWarnings("serial")
public final class ServerNewMatchEvent extends Event implements Serializable
{

	private final List<Role> roles;

	public ServerNewMatchEvent(List<Role> roles)
	{
		this.roles = roles;
	}

	public List<Role> getRoles()
	{
		return roles;
	}

}
