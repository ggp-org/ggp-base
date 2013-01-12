package org.ggp.base.player.gamer.event;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;

public final class GamerNewMatchEvent extends Event
{

	private final Match match;
	private final GdlConstant roleName;

	public GamerNewMatchEvent(Match match, GdlConstant roleName)
	{
		this.match = match;
		this.roleName = roleName;
	}

	public Match getMatch()
	{
		return match;
	}

	public GdlConstant getRoleName()
	{
		return roleName;
	}

}
