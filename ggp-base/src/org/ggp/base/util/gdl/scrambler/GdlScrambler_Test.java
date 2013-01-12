package org.ggp.base.util.gdl.scrambler;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import junit.framework.TestCase;

/**
 * Unit tests for the GdlScrambler class, which provides a way
 * to scramble and unscramble Gdl objects without changing the
 * underlying physics of the games they represent.
 * 
 * @author Sam
 */
public class GdlScrambler_Test extends TestCase {	
	/**
	 * When scrambling is enabled, the "MappingGdlScrambler" is used. This class
     * systematically replaces all of the constant and variable names in the Gdl
     * with scrambled versions, drawing new random tokens first from a list of
     * English words, and then generating random word-like strings when the list
     * has been exhausted.
	 */
    public void testMappingScrambler() throws GdlFormatException, SymbolFormatException {
    	runScramblerTest(new MappingGdlScrambler());
    }
    
    /**
     * When scrambling is disabled, the "NoOpGdlScrambler" is used. This class
     * simply renders the Gdl and parses it in the naive way, without doing any
     * special modification. This is the trivial case of "scrambling".
     */
    public void testNoOpScrambler() throws GdlFormatException, SymbolFormatException {
    	runScramblerTest(new NoOpGdlScrambler());
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