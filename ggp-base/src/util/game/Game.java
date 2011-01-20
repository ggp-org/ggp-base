package util.game;

import java.util.ArrayList;
import java.util.List;

import external.JSON.JSONObject;
import util.gdl.factory.GdlFactory;
import util.gdl.grammar.Gdl;
import util.symbol.factory.SymbolFactory;
import util.symbol.grammar.SymbolList;

/**
 * Game objects contain all of the relevant information about a specific game,
 * like Chess or Connect Four. This information includes the game's rules and
 * stylesheet, and maybe a human-readable description, and also any available
 * metadata, like the game's name and its associated game repository URL.
 * 
 * Games do not necessarily have all of these fields. Games loaded from local
 * storage will not have a repository URL, and probably will be missing other
 * metadata as well. Games sent over the wire from a game server rather than
 * loaded from a repository are called "emphemeral" games, and contain only
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
 * @author Sam
 */
public final class Game {
    private final String theKey;
    private final String theName;
    private final String theDescription;    
    private final String theRepositoryURL;
    private final String theStylesheet;
    private final List<Gdl> theRules;

    public static Game createEphemeralGame(List<Gdl> theRules) {
        return new Game(null, null, null, null, null, theRules);
    }

    protected Game (String theKey, String theName, String theDescription, String theRepositoryURL, String theStylesheet, List<Gdl> theRules) {
        this.theKey = theKey;
        this.theName = theName;
        this.theDescription = theDescription;
        this.theRepositoryURL = theRepositoryURL;
        this.theStylesheet = theStylesheet;
        this.theRules = theRules;
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

    public List<Gdl> getRules() {
        return theRules;
    }
    
    public String serializeToJSON() {
        try {
            JSONObject theGameObject = new JSONObject();
            theGameObject.put("theKey", getKey());
            theGameObject.put("theName", getName());
            theGameObject.put("theDescription", getDescription());
            theGameObject.put("theRepositoryURL", getRepositoryURL());
            theGameObject.put("theStylesheet", getStylesheet());
            
            // Serialize the rulesheet
            StringBuilder theProcessedRulesheet = new StringBuilder("( ");
            for (Gdl gdl : getRules()) {
                theProcessedRulesheet.append(gdl + " ");
            }
            theProcessedRulesheet.append(" )");
            theGameObject.put("theProcessedRulesheet", theProcessedRulesheet.toString());
            
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

            // Deserialize the rulesheet
            String theRulesheet = theGameObject.getString("theProcessedRulesheet");
            SymbolList ruleList = (SymbolList) SymbolFactory.create(theRulesheet);
            List<Gdl> theRules = new ArrayList<Gdl>();
            for (int i = 0; i < ruleList.size(); i++)
            {
                theRules.add(GdlFactory.create(ruleList.get(i)));
            } 
            
            return new Game(theKey, theName, theDescription, theRepositoryURL, theStylesheet, theRules);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }        
    }
}