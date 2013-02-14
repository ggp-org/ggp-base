package org.ggp.base.util.game;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.SymbolList;

import external.JSON.JSONObject;

/**
 * Game objects contain all of the relevant information about a specific game,
 * like Chess or Connect Four. This information includes the game's rules and
 * stylesheet, and maybe a human-readable description, and also any available
 * metadata, like the game's name and its associated game repository URL.
 * 
 * Games do not necessarily have all of these fields. Games loaded from local
 * storage will not have a repository URL, and probably will be missing other
 * metadata as well. Games sent over the wire from a game server rather than
 * loaded from a repository are called "ephemeral" games, and contain only
 * their rulesheet; they have no metadata, and do not even have unique keys.
 * 
 * Aside from ephemeral games, all games have a key that is unique within their
 * containing repository (either local storage or a remote repository). Games
 * can be indexed internally using this key. Whenever possible, the user should
 * be shown the game's name (if available) rather than the internal key, since
 * the game's name is more readable/informative than the key.
 * 
 * (e.g. A game with the name "Three-Player Free-For-All" but the key "3pffa".)
 * 
 * NOTE: Games are different from matches. Games represent the actual game
 * being played, whereas matches are particular instances in which players
 * played through the game. For example, you might have a Game object that
 * contains information about chess: it would contain the rules for chess,
 * methods for visualizing chess matches, a human readable description of
 * the rules of chess, and so on. On the other hand, for any particular
 * chess match between two players, you would have a Match object that has
 * a record of what moves were played, what states were transitioned through,
 * when everything happened, how the match was configured, and so on. There
 * can be many Match objects all associated with a single Game object, just
 * as there can be many matches played of a particular game.
 * 
 * NOTE: Games operate only on "processed" rulesheets, which have been stripped
 * of comments and are properly formatted as SymbolLists. Rulesheets which have
 * not been processed in this fashion will break the Game object. This processing
 * can be done by calling "Game.preprocessRulesheet" on the raw rulesheet. Note
 * that rules transmitted over the network are always processed.
 * 
 * @author Sam
 */

public final class Game {
    private final String theKey;
    private final String theName;
    private final String theDescription;    
    private final String theRepositoryURL;
    private final String theStylesheet;
    private final String theRulesheet;

    public static Game createEphemeralGame(String theRulesheet) {
        return new Game(null, null, null, null, null, theRulesheet);
    }

    protected Game (String theKey, String theName, String theDescription, String theRepositoryURL, String theStylesheet, String theRulesheet) {
        this.theKey = theKey;
        this.theName = theName;
        this.theDescription = theDescription;
        this.theRepositoryURL = theRepositoryURL;
        this.theStylesheet = theStylesheet;
        this.theRulesheet = theRulesheet;
    }

    public String getKey() {
        return theKey;
    }

    public String getName() {
        return theName;
    }

    public String getRepositoryURL() {
        return theRepositoryURL;
    }

    public String getDescription() {
        return theDescription;
    }

    public String getStylesheet() {
        return theStylesheet;
    }
    
    public String getRulesheet() {
    	return theRulesheet;
    }
    
    /**
     * Pre-process a rulesheet into the standard form. This involves stripping
     * comments and adding opening and closing parens so that the rulesheet is
     * a valid SymbolList. This must be done to any raw rulesheets coming from
     * the local disk or a repository server. This is always done to rulesheets
     * before they're stored in Game objects or sent over the network as part
     * of a START request.
     * 
     * @param raw rulesheet
     * @return processed rulesheet
     */
    public static String preprocessRulesheet(String rawRulesheet) {
		// First, strip all of the comments from the rulesheet.
		StringBuilder rulesheetBuilder = new StringBuilder();
		String[] rulesheetLines = rawRulesheet.split("[\n\r]");
		for (int i = 0; i < rulesheetLines.length; i++) {
			String line = rulesheetLines[i];
			int comment = line.indexOf(';');
			int cutoff = (comment == -1) ? line.length() : comment;
			rulesheetBuilder.append(line.substring(0, cutoff));
			rulesheetBuilder.append(" ");
		}
		String processedRulesheet = rulesheetBuilder.toString();
		
		// Add opening and closing parens for parsing as symbol list.
		processedRulesheet = "( " + processedRulesheet + " )";
		
		return processedRulesheet;
    }

    /**
     * Gets the GDL object representation of the game rulesheet. This representation
     * is generated when "getRules" is called, rather than when the game is created,
     * so that it's safe to drain the GDL pool between when the game repository is
     * loaded and when the games are actually used. This doesn't incur a performance
     * penalty because this method is usually called only once per match, when the
     * state machine is initialized -- as a result it's actually better to only parse
     * the rules when they're needed rather than parsing them for every game when the
     * game repository is created.
     * 
     * @return
     */
    public List<Gdl> getRules() {
    	try {
	        List<Gdl> rules = new ArrayList<Gdl>();
	        SymbolList list = (SymbolList) SymbolFactory.create(theRulesheet);
	        for (int i = 0; i < list.size(); i++)
	        {
	            rules.add(GdlFactory.create(list.get(i)));
	        }
	        return rules;
    	} catch (GdlFormatException e) {
    		e.printStackTrace();
    		return null;
    	} catch (SymbolFormatException e) {
    		e.printStackTrace();
    		return null;
    	}        
    }
    
    public String serializeToJSON() {
        try {
            JSONObject theGameObject = new JSONObject();
            theGameObject.put("theKey", getKey());
            theGameObject.put("theName", getName());
            theGameObject.put("theDescription", getDescription());
            theGameObject.put("theRepositoryURL", getRepositoryURL());
            theGameObject.put("theStylesheet", getStylesheet());
            theGameObject.put("theRulesheet", getRulesheet());
            
            return theGameObject.toString();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Game loadFromJSON(String theSerializedGame) {
        try {
            JSONObject theGameObject = new JSONObject(theSerializedGame);
            
            String theKey = null;
            try {
                theKey = theGameObject.getString("theKey");
            } catch (Exception e) {}
            
            String theName = null;
            try {
                theName = theGameObject.getString("theName");
            } catch (Exception e) {}
            
            String theDescription = null;
            try {
                theDescription = theGameObject.getString("theDescription");
            } catch (Exception e) {}

            String theRepositoryURL = null;
            try {
                theRepositoryURL = theGameObject.getString("theRepositoryURL");
            } catch (Exception e) {}
            
            String theStylesheet = null;
            try {
                theStylesheet = theGameObject.getString("theStylesheet");
            } catch (Exception e) {}

            String theRulesheet = null;
            try {
            	theRulesheet = theGameObject.getString("theRulesheet");
            } catch (Exception e) {}
            
            return new Game(theKey, theName, theDescription, theRepositoryURL, theStylesheet, theRulesheet);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }        
    }
}