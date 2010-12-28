package util.match;

import java.util.ArrayList;
import java.util.List;

import util.game.Game;
import util.gdl.grammar.GdlSentence;

/**
 * Match encapsulates all of the information relating to a single match.
 * A match is a single play through a game, with a complete history that
 * lists what move each player made at each step through the match. This
 * also includes other relevant metadata about the match, including some
 * unique identifiers, configuration information, and so on.
 * 
 * @author Sam
 */
public final class Match
{
    private final String matchId;
    private final int playClock;
    private final int startClock;
    private final long startTime;
	private final Game theGame;
	private final List<List<GdlSentence>> history;

	public Match(String matchId, int startClock, int playClock, long startTime, Game theGame)
	{
		this.matchId = matchId;
		this.startClock = startClock;
		this.playClock = playClock;		
		this.startTime = startTime;
		this.theGame = theGame;
		
		history = new ArrayList<List<GdlSentence>>();
	}

	public void appendMoves(List<GdlSentence> moves) {
		history.add(moves);
	}

	public Game getGame() {
		return theGame;
	}

	public List<List<GdlSentence>> getHistory() {
		return history;
	}
	
	public List<GdlSentence> getMostRecentMoves() {
		if (history.size() == 0)
			return null;
		
		return history.get(history.size()-1);
	}

	public String getMatchId() {
		return matchId;
	}

	public int getPlayClock() {
		return playClock;
	}

	public int getStartClock() {
		return startClock;
	}
	
	public long getStartTime() {
	    return startTime;
	}
}