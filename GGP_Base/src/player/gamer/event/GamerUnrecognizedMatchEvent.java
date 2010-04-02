package player.gamer.event;

import util.observer.Event;

public final class GamerUnrecognizedMatchEvent extends Event
{

	private final String matchId;

	public GamerUnrecognizedMatchEvent(String matchId)
	{
		this.matchId = matchId;
	}

	public String getMatchId()
	{
		return matchId;
	}

}
