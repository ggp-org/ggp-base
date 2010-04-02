package server.event;

import java.io.Serializable;

import util.observer.Event;
import util.statemachine.implementation.prover.ProverMachineState;

@SuppressWarnings("serial")
public final class ServerNewGameStateEvent extends Event implements Serializable
{
	private final ProverMachineState state;

	public ServerNewGameStateEvent(ProverMachineState state)
	{
		this.state = state;
	}

	public ProverMachineState getState()
	{
		return state;
	}
}