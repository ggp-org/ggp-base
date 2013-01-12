package org.ggp.base.util.gdl.grammar;

@SuppressWarnings("serial")
public final class GdlConstant extends GdlTerm
{

	private final String value;

	GdlConstant(String value)
	{
		this.value = value.intern();
	}

	public String getValue()
	{
		return value;
	}

	@Override
	public boolean isGround()
	{
		return true;
	}

	@Override
	public GdlSentence toSentence()
	{
		return GdlPool.getProposition(this);
	}

	@Override
	public String toString()
	{
		return value;
	}

}
