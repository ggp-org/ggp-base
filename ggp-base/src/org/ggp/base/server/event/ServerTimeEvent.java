package org.ggp.base.server.event;

import java.io.Serializable;

import org.ggp.base.util.observer.Event;


@SuppressWarnings("serial")
public final class ServerTimeEvent extends Event implements Serializable
{

	private final long time;

	public ServerTimeEvent(long time)
	{
		this.time = time;
	}

	public long getTime()
	{
		return time;
	}

}
