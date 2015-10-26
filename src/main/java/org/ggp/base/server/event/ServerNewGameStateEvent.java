package org.ggp.base.server.event;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.MachineState;


public final class ServerNewGameStateEvent extends Event
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