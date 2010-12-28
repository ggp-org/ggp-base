package util.match;

import java.util.ArrayList;
import java.util.List;

import util.gdl.grammar.Gdl;
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
	private final List<Gdl> description;
	private final List<List<GdlSentence>> history;

	public Match(String matchId, int startClock, int playClock, List<Gdl> description)
	{
		this.matchId = matchId;
		this.startClock = startClock;
		this.playClock = playClock;
		this.description = description;
		history = new ArrayList<List<GdlSentence>>();
	}

	public void appendMoves(List<GdlSentence> moves)
	{
		history.add(moves);
	}

	public List<Gdl> getDescription()
	{
		return description;
	}

	public List<List<GdlSentence>> getHistory()
	{
		return history;
	}
	
	public List<GdlSentence> getMostRecentMoves()
	{
		if(history.size()==0)
			return null;
		
		return history.get(history.size()-1);
	}

	public String getMatchId()
	{
		return matchId;
	}

	public int getPlayClock()
	{
		return playClock;
	}

	public int getStartClock()
	{
		return startClock;
	}
}