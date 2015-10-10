package org.ggp.base.util.game;

import org.junit.Assert;
import org.junit.Test;

public class GameParsingTest extends Assert {

    @Test
    public void testParseGame() throws Exception {
        StringBuilder theRulesheet = new StringBuilder();
        theRulesheet.append("; comment\n");
        theRulesheet.append("(a b)\n");
        theRulesheet.append("; comment two\n");
        theRulesheet.append("(c d e) ; comment three\n");
        theRulesheet.append("(f g)\n");
        theRulesheet.append("(h i j)\n");
        assertEquals(4, Game.createEphemeralGame(Game.preprocessRulesheet(theRulesheet.toString())).getRules().size());
    }

}
