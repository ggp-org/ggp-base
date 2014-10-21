package org.ggp.base.util.gdl.scrambler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the GdlScrambler class, which provides a way
 * to scramble and unscramble Gdl objects without changing the
 * underlying physics of the games they represent.
 *
 * @author Sam
 */
public class GdlScramblerTest extends Assert {
    /**
     * When scrambling is disabled, the "NoOpGdlScrambler" is used. This class
     * simply renders the Gdl and parses it in the naive way, without doing any
     * special modification. This is the trivial case of "scrambling".
     */
	@Test
    public void testNoOpScrambler() throws GdlFormatException, SymbolFormatException {
    	runScramblerTest(new NoOpGdlScrambler());
    }

	/**
	 * When scrambling is enabled, the "MappingGdlScrambler" is used. This class
     * systematically replaces all of the constant and variable names in the Gdl
     * with scrambled versions, drawing new random tokens first from a list of
     * English words, and appending suffixes when the original list is exhausted.
	 */
	@Test
    public void testMappingScrambler() throws GdlFormatException, SymbolFormatException {
    	runScramblerTest(new MappingGdlScrambler(new Random()));
    }

    /**
     * Furthermore, the mapping scrambler can be initialized with a Random object,
     * which can be used to ensure deterministic, reproducible scrambling. This can
     * be used to scramble a specific match in the same way, even if it stored and
     * reloaded in the meantime.
     */
	@Test
    public void testMappingScramblerConsistency() {
    	GdlScrambler aScrambler = new MappingGdlScrambler(new Random(123));
    	GdlScrambler bScrambler = new MappingGdlScrambler(new Random(123));
    	GdlScrambler cScrambler = new MappingGdlScrambler(new Random(234));
    	GameRepository repo = GameRepository.getDefaultRepository();
    	for (String gameKey : repo.getGameKeys()) {
    		Game game = repo.getGame(gameKey);
    		StringBuilder aScrambledRules = new StringBuilder();
    		StringBuilder bScrambledRules = new StringBuilder();
    		StringBuilder cScrambledRules = new StringBuilder();
    		StringBuilder dScrambledRules = new StringBuilder();
    		StringBuilder eScrambledRules = new StringBuilder();
    		StringBuilder fScrambledRules = new StringBuilder();
    		for(Gdl rule : game.getRules()) {
    			aScrambledRules.append(aScrambler.scramble(rule)+"\n");
    			bScrambledRules.append(bScrambler.scramble(rule)+"\n");
    			cScrambledRules.append(cScrambler.scramble(rule)+"\n");
    		}
    		for(Gdl rule : game.getRules()) {
    			dScrambledRules.append(aScrambler.scramble(rule)+"\n");
    			eScrambledRules.append(bScrambler.scramble(rule)+"\n");
    			fScrambledRules.append(cScrambler.scramble(rule)+"\n");
    		}
    		assertEquals(aScrambledRules.toString(), bScrambledRules.toString());
    		assertEquals(aScrambledRules.toString(), dScrambledRules.toString());
    		assertEquals(aScrambledRules.toString(), eScrambledRules.toString());
    		assertFalse(aScrambledRules.toString().equals(cScrambledRules.toString()));
    		assertEquals(cScrambledRules.toString(), fScrambledRules.toString());
    	}
    }

    private void runScramblerTest(GdlScrambler scrambler) throws SymbolFormatException, GdlFormatException {
    	GameRepository repo = GameRepository.getDefaultRepository();
    	for (String gameKey : repo.getGameKeys()) {
    		Game game = repo.getGame(gameKey);
    		List<Gdl> theScrambledRules = new ArrayList<Gdl>();
    		for(Gdl rule : game.getRules()) {
    			String renderedRule = rule.toString();
    			String renderedScrambledRule = scrambler.scramble(rule).toString();
    			String renderedUnscrambledRule = scrambler.unscramble(renderedScrambledRule).toString();
    			theScrambledRules.add(GdlFactory.create(renderedScrambledRule));
    			// If the scrambler claims that it scrambles the game, then the
    			// scrambled rules should be different than the original rules.
    			// Otherwise they should be identical.
    			if (scrambler.scrambles()) {
    				assertTrue(gameKey, !renderedRule.equals(renderedScrambledRule));
    			} else {
    				assertEquals(gameKey, renderedRule, renderedScrambledRule);
    			}
    			// One important property for any scrambler is that the original
    			// and the unscrambled Gdl must be the same. This guarantees that
    			// the server can correctly unscramble responses from the players.
    			assertEquals(gameKey, renderedRule, renderedUnscrambledRule);
    		}

			// An important property for any scrambler is that the scrambled rules
			// have the same physics as the regular rules. For example, the number
			// of roles in each game should be the same, and the number of facts
    		// that are true in the initial state should be the same. There could
    		// be more thorough verification here, like looking at the number of
    		// legal joint moves in the first state, or simulating entire matches,
    		// but that would be expensive.
			ProverStateMachine pNormal = new ProverStateMachine();
			ProverStateMachine pScrambled = new ProverStateMachine();
			pNormal.initialize(game.getRules());
			pScrambled.initialize(theScrambledRules);
			assertEquals(gameKey, pNormal.getRoles().size(), pScrambled.getRoles().size());
			assertEquals(gameKey, pNormal.getInitialState().getContents().size(), pScrambled.getInitialState().getContents().size());
    	}
    }
}