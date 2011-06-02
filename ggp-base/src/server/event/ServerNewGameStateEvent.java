package server.event;

import java.io.Serializable;

import util.observer.Event;
import util.statemachine.MachineState;

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