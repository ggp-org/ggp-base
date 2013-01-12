package org.ggp.base.player.event;

import org.ggp.base.util.observer.Event;

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
