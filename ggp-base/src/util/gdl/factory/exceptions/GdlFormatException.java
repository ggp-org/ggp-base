package util.gdl.factory.exceptions;

import util.symbol.grammar.Symbol;

@SuppressWarnings("serial")
public final class GdlFormatException extends Exception
{

	private final Symbol source;

	public GdlFormatException(Symbol source)
	{
		this.source = source;
	}

	public Symbol getSource()
	{
		return source;
	}

	@Override
	public String toString()
	{
		return "Improperly formatted gdl expression: " + source;
	}

}
