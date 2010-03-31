package util.statemachine.implementation.prover;

import java.io.Serializable;

import util.gdl.grammar.GdlSentence;
import util.statemachine.Move;

@SuppressWarnings("serial")
public final class ProverMove implements Move, Serializable
{

	private final GdlSentence contents;

	public ProverMove(GdlSentence contents)
	{
		this.contents = contents;
	}

	@Override
	public boolean equals(Object o)
	{
		if ((o != null) && (o instanceof ProverMove))
		{
			ProverMove move = (ProverMove) o;
			return move.contents.equals(contents);
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see util.statemachine.prover.Move#getContents()
	 */
	public GdlSentence getContents()
	{
		return contents;
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
