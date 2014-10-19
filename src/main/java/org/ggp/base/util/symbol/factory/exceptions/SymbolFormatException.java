package org.ggp.base.util.symbol.factory.exceptions;

@SuppressWarnings("serial")
public final class SymbolFormatException extends Exception
{

	private final String source;

	public SymbolFormatException(String source)
	{
		this.source = source;
	}

	public String getSource()
	{
		return source;
	}

	@Override
	public String toString()
	{
		return "Improperly formatted symbolic expression: " + source;
	}

}
