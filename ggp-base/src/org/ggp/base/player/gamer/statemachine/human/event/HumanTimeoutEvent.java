package org.ggp.base.player.gamer.statemachine.human.event;

import org.ggp.base.player.gamer.statemachine.human.HumanGamer;
import org.ggp.base.util.observer.Event;


public final class HumanTimeoutEvent extends Event
{

	private final HumanGamer humanPlayer;

	public HumanTimeoutEvent(HumanGamer humanPlayer)
	{
		this.humanPlayer = humanPlayer;
	}

	public HumanGamer getHumanPlayer()
	{
		return humanPlayer;
	}

}
