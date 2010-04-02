package util.match;

import java.util.ArrayList;
import java.util.List;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlSentence;

public final class Match
{

	private final List<Gdl> description;
	private final List<List<GdlSentence>> history;
	private String matchId;
	private final int playClock;
	private final int startClock;

	public Match()
	{
		matchId = "";
		startClock = 0;
		playClock = 0;
		description = new ArrayList<Gdl>();
		history = new ArrayList<List<GdlSentence>>();
	}

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
	
	public void setMatchId(String matchId)
	{
		this.matchId = matchId; 
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
