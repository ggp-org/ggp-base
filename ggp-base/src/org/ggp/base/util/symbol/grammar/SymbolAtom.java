package org.ggp.base.util.symbol.grammar;

public final class SymbolAtom extends Symbol
{

	private final String value;

	SymbolAtom(String value)
	{
		this.value = value.intern();
	}

	public String getValue()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return value;
	}

}
