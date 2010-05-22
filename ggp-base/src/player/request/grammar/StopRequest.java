package player.request.grammar;

import player.gamer.Gamer;
import player.gamer.event.GamerCompletedMatchEvent;
import player.gamer.event.GamerUnrecognizedMatchEvent;
import util.logging.GamerLogger;

public final class StopRequest extends Request
{
	private final Gamer gamer;
	private final String matchId;

	public StopRequest(Gamer gamer, String matchId)
	{
		this.gamer = gamer;
		this.matchId = matchId;
	}
	
	@Override
	public String getMatchId() {
		return matchId;
	}

	@Override
	public String process(long receptionTime)
	{
        // First, check to ensure that this stop request is for the match
        // we're currently playing. If we're not playing a match, or we're
        // playing a different match, send back "busy".	    
		if (gamer.getMatch() == null || !gamer.getMatch().getMatchId().equals(matchId))
		{
		    GamerLogger.logError("GamePlayer", "Got stop message not intended for current game: ignoring.");
			gamer.notifyObservers(new GamerUnrecognizedMatchEvent(matchId));
			return "busy";
		}

		gamer.notifyObservers(new GamerCompletedMatchEvent());
		
		// Once the match has ended, set 'roleName' and 'match'
		// to NULL to indicate that we're ready to begin a new match.
		gamer.setRoleName(null);
	    gamer.setMatch(null);
	    
		return "done";
	}

	@Override
	public String toString()
	{
		return "stop";
	}
}