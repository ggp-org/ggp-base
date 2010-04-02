package player.gamer.statemachine.human.event;

import player.gamer.statemachine.human.HumanGamer;
import util.observer.Event;

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
