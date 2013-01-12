package org.ggp.base.test;

import junit.framework.Assert;

import org.ggp.base.util.game.Game;
import org.junit.Test;


public class GameParsingTests {

    @Test
    public void parseGame() throws Exception {
        StringBuilder theRulesheet = new StringBuilder();
        theRulesheet.append("; comment\n");
        theRulesheet.append("(a b)\n");
        theRulesheet.append("; comment two\n");
        theRulesheet.append("(c d e) ; comment three\n");
        theRulesheet.append("(f g)\n");
        theRulesheet.append("(h i j)\n");
        Assert.assertEquals(4, Game.createEphemeralGame(Game.preprocessRulesheet(theRulesheet.toString())).getRules().size());
    }

}
