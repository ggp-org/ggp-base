package org.ggp.base.util.gdl.grammar;

import java.util.List;

@SuppressWarnings("serial")
public final class GdlOr extends GdlLiteral
{

	private final List<GdlLiteral> disjuncts;
	private transient Boolean ground;

	GdlOr(List<GdlLiteral> disjuncts)
	{
		this.disjuncts = disjuncts;
		ground = null;
	}

	public int arity()
	{
		return disjuncts.size();
	}

	private boolean computeGround()
	{
		for (GdlLiteral literal : disjuncts)
		{
			if (!literal.isGround())
			{
				return false;
			}
		}

		return true;
	}

	public GdlLiteral get(int index)
	{
		return disjuncts.get(index);
	}

	@Override
	public boolean isGround()
	{
		if (ground == null)
		{
			ground = computeGround();
		}

		return ground;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("( or ");
		for (GdlLiteral literal : disjuncts)
		{
			sb.append(literal + " ");
		}
		sb.append(")");

		return sb.toString();
	}

}
