package org.ggp.base.player.gamer.python;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.python.stubs.SamplePythonGamerStub;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.match.Match;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the PythonGamer class, to verify that we can actually
 * instantiate a Python-based gamer and have it play moves in a game.
 *
 * @author Sam
 */
public class PythonGamerTest extends Assert {
	@Test
    public void testPythonGamer() {
        try {
            Gamer g = new SamplePythonGamerStub();
            assertEquals("SamplePythonGamer", g.getName());

            Match m = new Match("", -1, 1000, 1000, GameRepository.getDefaultRepository().getGame("ticTacToe"));
            g.setMatch(m);
            g.setRoleName(GdlPool.getConstant("xplayer"));
            g.metaGame(1000);
            assertTrue(g.selectMove(1000) != null);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}