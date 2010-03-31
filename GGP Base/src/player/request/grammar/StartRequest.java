package player.request.grammar;

import java.util.List;

import player.event.PlayerTimeEvent;
import player.gamer.Gamer;
import player.gamer.event.GamerNewMatchEvent;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlProposition;
import util.logging.GamerLogger;
import util.match.Match;

public final class StartRequest extends Request
{
	private final List<Gdl> description;
	private final Gamer gamer;
	private final String matchId;
	private final int playClock;
	private final GdlProposition roleName;
	private final int startClock;

	public StartRequest(Gamer gamer, String matchId, GdlProposition roleName, List<Gdl> description, int startClock, int playClock)
	{
		this.gamer = gamer;
		this.matchId = matchId;
		this.roleName = roleName;
		this.description = description;
		this.startClock = startClock;
		this.playClock = playClock;
	}

	@Override
	public String process(long receptionTime)
	{
		Match match = new Match(matchId, startClock, playClock, description);

		gamer.setMatch(match);
		gamer.setRoleName(roleName);
		gamer.notifyObservers(new GamerNewMatchEvent(match, roleName));

		try
		{
			gamer.notifyObservers(new PlayerTimeEvent(gamer.getMatch().getStartClock() * 1000));
			gamer.metaGame(gamer.getMatch().getStartClock() * 1000 + receptionTime);
		}
		catch (Exception e)
		{		    
		    GamerLogger.logStackTrace("Gamer", e);		    
			gamer.setMatch(new Match());			
			return "(not ready)";
		}

		return "ready";
	}

	@Override
	public String toString()
	{
		return "start";
	}
}