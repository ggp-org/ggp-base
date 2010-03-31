package player.gamer.event;

import util.gdl.grammar.GdlProposition;
import util.match.Match;
import util.observer.Event;

public final class GamerNewMatchEvent extends Event
{

	private final Match match;
	private final GdlProposition roleName;

	public GamerNewMatchEvent(Match match, GdlProposition roleName)
	{
		this.match = match;
		this.roleName = roleName;
	}

	public Match getMatch()
	{
		return match;
	}

	public GdlProposition getRoleName()
	{
		return roleName;
	}

}
