package util.statemachine.implementation.propnet;

import util.gdl.grammar.GdlSentence;
import util.statemachine.Move;

public class PropNetMove implements Move {	
	private GdlSentence contents;
	
	public PropNetMove(GdlSentence contents)
	{
		this.contents = contents;
	}
	
	@Override
	public GdlSentence getContents() {
		return contents;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if ((o != null) && (o instanceof PropNetMove))
		{
			PropNetMove move = (PropNetMove) o;
			return move.contents.equals(contents);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return contents.hashCode();
	}

	@Override
	public String toString()
	{
		return contents.toString();
	}
}