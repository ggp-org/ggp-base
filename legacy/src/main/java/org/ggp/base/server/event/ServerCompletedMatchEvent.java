package org.ggp.base.server.event;

import java.io.Serializable;
import java.util.List;

import org.ggp.base.util.observer.Event;


@SuppressWarnings("serial")
public final class ServerCompletedMatchEvent extends Event implements Serializable
{

	private final List<Integer> goals;

	public ServerCompletedMatchEvent(List<Integer> goals)
	{
		this.goals = goals;
	}

	public List<Integer> getGoals()
	{
		return goals;
	}

}
