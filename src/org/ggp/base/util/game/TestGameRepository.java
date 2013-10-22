package org.ggp.base.util.game;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.ggp.base.util.files.FileUtils;


/**
 * Test game repository that provides rulesheet-only access to games with no
 * associated metadata or other resources, to be used only for unit tests.
 * 
 * @author Sam
 */
public final class TestGameRepository extends GameRepository {
    protected Set<String> getUncachedGameKeys() {
        Set<String> theKeys = new HashSet<String>();
        for(File game : new File("games/test").listFiles()) {
            if(!game.getName().endsWith(".kif")) continue;
            theKeys.add(game.getName().replace(".kif", ""));
        }
        return theKeys;
    }
    
    protected Game getUncachedGame(String theKey) {
    	try {
    		return Game.createEphemeralGame(Game.preprocessRulesheet(FileUtils.readFileAsString(new File("games/test/" + theKey + ".kif"))));
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}        
    }
}