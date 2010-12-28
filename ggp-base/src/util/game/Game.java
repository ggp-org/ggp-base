package util.game;

import java.util.List;
import util.gdl.grammar.Gdl;

/**
 * Game objects contain all of the relevant information about a specific game,
 * like Chess or Connect Four. This information includes the game's rules and
 * stylesheet, and maybe a human-readable description, and also any available
 * metadata, like the game's name and its associated game repository URL.
 * 
 * Games do not necessarily have all of these fields. Games loaded from local
 * storage will not have a repository URL, and probably will be missing other
 * metadata as well.
 * 
 * However, all games are guaranteed to have a key that is unique within their
 * containing repository (either local storage or a remote repository). Games
 * can be indexed internally using this key. Whenever possible, the user should
 * be shown the game's name (if available) rather than the internal key, since
 * the game's name is more readable/informative than the key.
 * 
 * There is one case in which a game will not have a key: if it has been sent
 * from a server, rather than loaded from a repository (local or remote). These
 * games are "ephemeral", and contain only their rules.
 * 
 * (e.g. A game with the name "Three-Player Free-For-All" but the key "3pffa".)
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
}