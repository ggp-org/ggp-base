package org.ggp.base.server.event;

import java.io.Serializable;

import org.ggp.base.util.observer.Event;

@SuppressWarnings("serial")
public final class ServerAbortedMatchEvent extends Event implements Serializable
{
	public ServerAbortedMatchEvent()
	{
		;
	}
}
