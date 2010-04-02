package player.event;

import util.observer.Event;

public final class PlayerReceivedMessageEvent extends Event
{

	private final String message;

	public PlayerReceivedMessageEvent(String message)
	{
		this.message = message;
	}

	public String getMessage()
	{
		return message;
	}

}
