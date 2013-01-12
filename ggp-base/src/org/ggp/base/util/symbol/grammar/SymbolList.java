package org.ggp.base.util.symbol.grammar;

import java.util.List;

public final class SymbolList extends Symbol
{

	private final List<Symbol> contents;

	SymbolList(List<Symbol> contents)
	{
		this.contents = contents;
	}

	public Symbol get(int index)
	{
		return contents.get(index);
	}

	public int size()
	{
		return contents.size();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("( ");
		for (Symbol symbol : contents)
		{
			sb.append(symbol.toString() + " ");
		}
		sb.append(")");

		return sb.toString();
	}

}
