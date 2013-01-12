package org.ggp.base.util.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Game repositories contain games, and provide two main services: you can
 * query a repository to get a list of available games (by key), and given
 * a key, you can look up the associated Game object.
 * 
 * All queries to a game repository are cached, and the caching is handled
 * in this abstract base class. Concrete subclasses will implement the actual
 * behavior required for fetching games from the underlying repositories.
 * 
 * @author Sam
 */
public abstract class GameRepository {
    public static GameRepository getDefaultRepository() {
        return new CloudGameRepository("games.ggp.org/base");
    }
    
    public Game getGame(String theKey) {
        if (!theGames.containsKey(theKey)) {
            Game theGame = getUncachedGame(theKey);
            if (theGame != null) {
                theGames.put(theKey, theGame);
            }
        }
        return theGames.get(theKey);
    }

    public Set<String> getGameKeys() {
        if (theGameKeys == null) {
            theGameKeys = getUncachedGameKeys();
        }
        return theGameKeys;
    }
    
    // Abstract methods, for implementation classes.
    protected abstract Game getUncachedGame(String theKey);
    protected abstract Set<String> getUncachedGameKeys();
    
    // Cached values, lazily filled.
    private Set<String> theGameKeys;
    private Map<String, Game> theGames = new HashMap<String, Game>();    
}