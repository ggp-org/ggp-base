package util.game;

import java.util.List;
import java.util.Set;

import util.configuration.LocalResourceLoader;
import util.gdl.grammar.Gdl;

/**
 * Local game repositories provide access to game resources stored on the
 * local disk, bundled with the GGP Base project.
 * 
 * @author Sam
 */
public final class LocalGameRepository extends GameRepository {    
    public Set<String> getUncachedGameKeys() {
        // TODO: Fill in this stub!
        return null;
    }
    
    public Game getUncachedGame(String theKey) {
        String stylesheet = LocalResourceLoader.loadStylesheet(theKey);
        List<Gdl> theRules = LocalResourceLoader.loadGame(theKey);
        return new Game(theKey, null, null, null, stylesheet, theRules);
    }
}