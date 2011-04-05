package util.game;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.configuration.LocalResourceLoader;
import util.configuration.ProjectConfiguration;
import util.gdl.grammar.Gdl;

/**
 * Local game repositories provide access to game resources stored on the
 * local disk, bundled with the GGP Base project.
 * 
 * @author Sam
 */
public final class LocalGameRepository extends GameRepository {    
    protected Set<String> getUncachedGameKeys() {
        Set<String> theKeys = new HashSet<String>();
        for(File game : ProjectConfiguration.gameRulesheetsDirectory.listFiles()) {
            if(!game.getName().endsWith(".kif")) continue;
            theKeys.add(game.getName().replace(".kif", ""));
        }
        return theKeys;
    }
    
    protected Game getUncachedGame(String theKey) {
        List<Gdl> theRules = LocalResourceLoader.loadGame(theKey);
        return new Game(theKey, null, null, null, null, theRules);
    }
}