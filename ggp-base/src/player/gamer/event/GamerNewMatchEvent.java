package player.gamer.event;

import util.gdl.grammar.GdlConstant;
import util.match.Match;
import util.observer.Event;

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
