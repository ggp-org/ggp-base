package org.ggp.base.util.gdl.factory.exceptions;

import org.ggp.base.util.symbol.grammar.Symbol;

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
