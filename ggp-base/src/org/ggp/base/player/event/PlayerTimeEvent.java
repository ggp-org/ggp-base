package org.ggp.base.player.event;

import org.ggp.base.util.observer.Event;

public final class PlayerTimeEvent extends Event
{

	private final long time;

	public PlayerTimeEvent(long time)
	{
		this.time = time;
	}

	public long getTime()
	{
		return time;
	}

}
