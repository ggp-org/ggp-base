package org.ggp.base.util.gdl.scrambler;

import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

public interface GdlScrambler {
	public String scramble(Gdl x);
	public Gdl unscramble(String x) throws SymbolFormatException, GdlFormatException;
	public boolean scrambles();
}