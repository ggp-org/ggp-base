package util.match;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
    private final String randomToken;
    private final int playClock;
    private final int startClock;
    private final Date startTime;
	private final Game theGame;
	private final List<List<GdlSentence>> history;

	public Match(String matchId, int startClock, int playClock, Game theGame)
	{
		this.matchId = matchId;
		this.startClock = startClock;
		this.playClock = playClock;
		this.theGame = theGame;
		
		this.startTime = new Date();
		this.randomToken = getRandomString(32);
		
		history = new ArrayList<List<GdlSentence>>();
	}
	
	/* Mutators */

	public void appendMoves(List<GdlSentence> moves) {
		history.add(moves);
	}
	
	/* Complex accessors */
	
    public List<GdlSentence> getMostRecentMoves() {
        if (history.size() == 0)
            return null;
        
        return history.get(history.size()-1);
    }
    
    public String getGameName() {
        return getGame().getName();
    }
    
    public String getGameRepositoryURL() {
        return getGame().getRepositoryURL();
    }
	
	/* Simple accessors */

    public String getMatchId() {
        return matchId;
    }
    
    public String getRandomToken() {
        return randomToken;
    }
	
	public Game getGame() {
		return theGame;
	}

	public List<List<GdlSentence>> getHistory() {
		return history;
	}

	public int getPlayClock() {
		return playClock;
	}

	public int getStartClock() {
		return startClock;
	}
	
	public Date getStartTime() {
	    return startTime;
	}
	
	/* Static methods */
	
    public static String getRandomString(int nLength) {
        Random theGenerator = new Random();
        String theString = "";
        for (int i = 0; i < nLength; i++) {
            int nVal = theGenerator.nextInt(62);
            if (nVal < 26) theString += (char)('a' + nVal);
            else if (nVal < 52) theString += (char)('A' + (nVal-26));
            else if (nVal < 62) theString += (char)('0' + (nVal-52));
        }
        return theString;
    }	
}