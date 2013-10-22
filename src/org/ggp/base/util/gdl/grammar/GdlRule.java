package org.ggp.base.util.gdl.grammar;

import java.util.List;

@SuppressWarnings("serial")
public final class GdlRule extends Gdl
{

	private final List<GdlLiteral> body;
	private transient Boolean ground;
	private final GdlSentence head;

	GdlRule(GdlSentence head, List<GdlLiteral> body)
	{
		this.head = head;
		this.body = body;
		ground = null;
	}

	public int arity()
	{
		return body.size();
	}

	private Boolean computeGround()
	{
		for (GdlLiteral literal : body)
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
		return body.get(index);
	}

	public GdlSentence getHead()
	{
		return head;
	}
	
	public List<GdlLiteral> getBody()
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
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("( <= " + head + " ");
		for (GdlLiteral literal : body)
		{
			sb.append(literal + " ");
		}
		sb.append(")");

		return sb.toString();
	}

}
