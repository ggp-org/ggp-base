package player.request.grammar;

import java.util.List;

import player.event.PlayerTimeEvent;
import player.gamer.Gamer;
import player.gamer.event.GamerUnrecognizedMatchEvent;
import util.gdl.grammar.GdlTerm;
import util.logging.GamerLogger;

public final class PlayRequest extends Request
{
	private final Gamer gamer;
	private final String matchId;
	private final List<GdlTerm> moves;

	public PlayRequest(Gamer gamer, String matchId, List<GdlTerm> moves)
	{
		this.gamer = gamer;
		this.matchId = matchId;
		this.moves = moves;
	}

	@Override
	public String getMatchId() {
		return matchId;
	}
	
	@Override
	public String process(long receptionTime)
	{
	    // First, check to ensure that this play request is for the match
	    // we're currently playing. If we're not playing a match, or we're
	    // playing a different match, send back "busy".
		if (gamer.getMatch() == null || !gamer.getMatch().getMatchId().equals(matchId)) {
			gamer.notifyObservers(new GamerUnrecognizedMatchEvent(matchId));
			GamerLogger.logError("GamePlayer", "Got play message not intended for current game: ignoring.");
			return "busy";
		}

		if (moves != null) {
			gamer.getMatch().appendMoves(moves);
		}

		try {
			gamer.notifyObservers(new PlayerTimeEvent(gamer.getMatch().getPlayClock() * 1000));
			return gamer.selectMove(gamer.getMatch().getPlayClock() * 1000 + receptionTime).toString();
		} catch (Exception e) {
		    GamerLogger.logStackTrace("GamePlayer", e);
			return "nil";
		}
	}

	@Override
	public String toString()
	{
		return "play";
	}
}