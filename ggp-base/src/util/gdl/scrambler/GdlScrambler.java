package util.gdl.scrambler;

import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.symbol.factory.exceptions.SymbolFormatException;

public interface GdlScrambler {	
	public String scramble(Gdl x);
	public Gdl unscramble(String x) throws SymbolFormatException, GdlFormatException;
	public boolean scrambles();
}