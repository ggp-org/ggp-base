package util.gdl.model;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlSentence;

public interface SentenceForm {

	GdlConstant getName();

	SentenceForm getCopyWithName(GdlConstant name);

	boolean matches(GdlSentence relation);
	/**
	 * Returns the number of constants and/or variables that a sentence
	 * of this form contains. 
	 */
	int getTupleSize();

}
