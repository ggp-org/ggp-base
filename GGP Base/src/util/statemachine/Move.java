package util.statemachine;

import util.gdl.grammar.GdlSentence;

public interface Move {
	/**
	 * Returns the contents of the move
	 * represented as a GdlSentence.
	 * @return move contents
	 */
	public GdlSentence getContents();

}