package util.game;

import java.util.Set;

/**
 * Game repositories contain games, and provide two main services: you can
 * query a repository to get a list of available games (by key), and given
 * a key, you can look up the associated Game object.
 * 
 * @author Sam
 */
public interface GameRepository {
    public Set<String> getGameKeys();
    public Game getGame(String theKey);
}