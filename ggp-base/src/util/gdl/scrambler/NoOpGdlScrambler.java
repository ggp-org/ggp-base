package util.gdl.scrambler;

import util.gdl.factory.GdlFactory;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.symbol.factory.exceptions.SymbolFormatException;

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