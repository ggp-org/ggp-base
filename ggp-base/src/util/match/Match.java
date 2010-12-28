package util.match;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import util.game.Game;
import util.gdl.grammar.GdlSentence;
import util.statemachine.Role;

/**
 * Match encapsulates all of the information relating to a single match.
 * A match is a single play through a game, with a complete history that
 * lists what move each player made at each step through the match. This
 * also includes other relevant metadata about the match, including some
 * unique identifiers, configuration information, and so on.
 * 
 * NOTE: Match objects created by a player, representing state read from
 * a server, are not completely filled out. For example, they only get an
 * ephemeral Game object, which has a rulesheet but no key or metadata.
 * Gamers which do not derive from StateMachineGamer also do not keep any
 * information on what states have been observed, because (somehow) they
 * are representing games without using state machines. In general, these
 * player-created Match objects shouldn't be sent out into the ecosystem.
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
	private final List<String> theRoleNames;
	private final List<List<GdlSentence>> moveHistory;
	private final List<Set<GdlSentence>> stateHistory;	
	private final List<Date> stateTimeHistory;
	private boolean isCompleted;

	public Match(String matchId, int startClock, int playClock, Game theGame)
	{
		this.matchId = matchId;
		this.startClock = startClock;
		this.playClock = playClock;
		this.theGame = theGame;
		
		this.startTime = new Date();
		this.randomToken = getRandomString(32);
		this.isCompleted = false;
		
		this.theRoleNames = new ArrayList<String>();
		for(Role r : Role.computeRoles(theGame.getRules())) {
		    this.theRoleNames.add(r.getName().getName().toString());
		}		
		
		this.moveHistory = new ArrayList<List<GdlSentence>>();
		this.stateHistory = new ArrayList<Set<GdlSentence>>();
		this.stateTimeHistory = new ArrayList<Date>();
	}
	
	/* Mutators */

	public void appendMoves(List<GdlSentence> moves) {
		moveHistory.add(moves);
	}
	
	public void appendState(Set<GdlSentence> state) {
	    stateHistory.add(state);
	    stateTimeHistory.add(new Date());
	}
	
	public void markCompleted() {
	    isCompleted = true;
	}
	
	/* Complex accessors */
	
    public List<GdlSentence> getMostRecentMoves() {
        if (moveHistory.size() == 0)
            return null;
        return moveHistory.get(moveHistory.size()-1);
    }

    public Set<GdlSentence> getMostRecentState() {
        if (stateHistory.size() == 0)
            return null;
        return stateHistory.get(stateHistory.size()-1);        
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

	public List<List<GdlSentence>> getMoveHistory() {
		return moveHistory;
	}
	
    public List<Set<GdlSentence>> getStateHistory() {
        return stateHistory;
    }
    
    public List<Date> getStateTimeHistory() {
        return stateTimeHistory;
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
	
	public List<String> getRoleNames() {
	    return theRoleNames;
	}
	
	public boolean isCompleted() {
	    return isCompleted;
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