package util.game;

import java.util.HashSet;
import java.util.Set;

/**
 * Local game repositories provide access to game resources stored on the
 * local disk, bundled with the GGP Base project.
 * 
 * TODO: Fill in this stub!
 * 
 * @author Sam
 */
public class LocalGameRepository implements GameRepository {
    public LocalGameRepository() {
        ;
    }
    public Set<String> getGameKeys() {
        return new HashSet<String>();
    }
    public Game getGame(String theKey) {
        return null;
    }
}