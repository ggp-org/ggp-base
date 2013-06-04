package org.ggp.base.validator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;

public final class OPNFValidator implements GameValidator
{
	@Override
	public void checkValidity(Game theGame) throws ValidatorException {
        PrintStream stdout = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));                                
		try {
			if (OptimizingPropNetFactory.create(theGame.getRules()) == null) {
				throw new ValidatorException("Got null result from OPNF");
			}
		} catch (Exception e) {
			throw new ValidatorException("OPNF Exception: " + e);
		} finally {
	        System.setOut(stdout);
		}
	}
}
