package org.ggp.base.player.request.grammar;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.exception.GameAnalysisException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.logging.GamerLogger;

public final class AnalyzeRequest extends Request
{
	private final Game game;
	private final Gamer gamer;
	private final int analysisClock;

	public AnalyzeRequest(Gamer gamer, Game theGame, int analysisClock)
	{
		this.gamer = gamer;
		this.game = theGame;
		this.analysisClock = analysisClock;
	}
	
	@Override
	public String getMatchId() {
		return null;
	}	
	
	@Override
	public String process(long receptionTime)
	{
		// Ensure that we aren't already playing a match. If we are,
	    // ignore the message, saying that we're busy.
		if (gamer.getMatch() != null) {
            GamerLogger.logError("GamePlayer", "Got analyze message while already busy playing a game: ignoring.");
            //gamer.notifyObservers(new GamerUnrecognizedMatchEvent(matchId));
            return "busy";
        }
	    
		// Otherwise, if we're not busy, have the gamer start analyzing.
		try {
			//gamer.notifyObservers(new PlayerTimeEvent(gamer.getMatch().getStartClock() * 1000));
			gamer.analyze(game, analysisClock * 1000 + receptionTime);
			//gamer.metaGame(gamer.getMatch().getStartClock() * 1000 + receptionTime);
		} catch (GameAnalysisException e) {		    
		    GamerLogger.logStackTrace("GamePlayer", e);

		    // Upon encountering an uncaught exception during analysis,
		    // assume that indicates that we aren't actually able to play
		    // right now, and tell the server that we're busy.
			return "busy";
		}

		return "ready";
	}

	@Override
	public String toString()
	{
		return "start";
	}
}