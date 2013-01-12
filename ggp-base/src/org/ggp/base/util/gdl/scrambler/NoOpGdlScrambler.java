package org.ggp.base.util.gdl.scrambler;

import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

public class NoOpGdlScrambler implements GdlScrambler {
	@Override
	public String scramble(Gdl x) {
		return x.toString();
	}
	@Override
	public Gdl unscramble(String x) throws SymbolFormatException, GdlFormatException {
		return GdlFactory.create(x);
	}
	@Override
	public boolean scrambles() {
		return false;
	}
}