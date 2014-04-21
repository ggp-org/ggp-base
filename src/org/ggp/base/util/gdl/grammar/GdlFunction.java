package org.ggp.base.util.gdl.grammar;

import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("serial")
public final class GdlFunction extends GdlTerm
{

	private final ImmutableList<GdlTerm> body;
	private transient Boolean ground;
	private final GdlConstant name;

	GdlFunction(GdlConstant name, ImmutableList<GdlTerm> body)
	{
		this.name = name;
		this.body = body;
		ground = null;
	}

	public int arity()
	{
		return body.size();
	}

	private boolean computeGround()
	{
		for (GdlTerm term : body)
		{
			if (!term.isGround())
			{
				return false;
			}
		}

		return true;
	}

	public GdlTerm get(int index)
	{
		return body.get(index);
	}

	public GdlConstant getName()
	{
		return name;
	}

	public List<GdlTerm> getBody()
	{
		return body;
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
	public GdlSentence toSentence()
	{
		return GdlPool.getRelation(name, body);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("( " + name + " ");
		for (GdlTerm term : body)
		{
			sb.append(term + " ");
		}
		sb.append(")");

		return sb.toString();
	}

}
