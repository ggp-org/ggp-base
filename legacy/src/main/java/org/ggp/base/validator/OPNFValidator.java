package org.ggp.base.validator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;

import com.google.common.collect.ImmutableList;

public final class OPNFValidator implements GameValidator
{
	@Override
	public List<ValidatorWarning> checkValidity(Game theGame) throws ValidatorException {
        PrintStream stdout = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
		try {
			if (OptimizingPropNetFactory.create(theGame.getRules()) == null) {
				throw new ValidatorException("Got null result from OPNF");
			}
		} catch (Exception e) {
			throw new ValidatorException("OPNF Exception: " + e, e);
		} finally {
	        System.setOut(stdout);
		}
		return ImmutableList.of();
	}
}
