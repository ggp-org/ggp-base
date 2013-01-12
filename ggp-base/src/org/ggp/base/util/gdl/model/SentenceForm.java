package org.ggp.base.util.gdl.model;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;

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
