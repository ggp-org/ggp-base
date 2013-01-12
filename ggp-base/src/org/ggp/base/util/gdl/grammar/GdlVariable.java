package org.ggp.base.util.gdl.grammar;

@SuppressWarnings("serial")
public final class GdlVariable extends GdlTerm
{

	private final String name;

	GdlVariable(String name)
	{
		this.name = name.intern();
	}

	public String getName()
	{
		return name;
	}

	@Override
	public boolean isGround()
	{
		return false;
	}

	@Override
	public GdlSentence toSentence()
	{
		throw new RuntimeException("Unable to convert a GdlVariable to a GdlSentence!");
	}

	@Override
	public String toString()
	{
		return name;
	}

}
