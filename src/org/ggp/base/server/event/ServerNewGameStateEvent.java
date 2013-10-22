package org.ggp.base.server.event;

import java.io.Serializable;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.MachineState;


@SuppressWarnings("serial")
public final class ServerNewGameStateEvent extends Event implements Serializable
{
	private final MachineState state;

	public ServerNewGameStateEvent(MachineState state)
	{
		this.state = state;
	}

	public MachineState getState()
	{
		return state;
	}
}