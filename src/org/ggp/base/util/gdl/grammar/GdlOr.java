package org.ggp.base.util.gdl.grammar;

import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("serial")
public final class GdlOr extends GdlLiteral
{

	private final ImmutableList<GdlLiteral> disjuncts;
	private transient Boolean ground;

	GdlOr(ImmutableList<GdlLiteral> disjuncts)
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

	public List<GdlLiteral> getDisjuncts() {
		return disjuncts;
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
