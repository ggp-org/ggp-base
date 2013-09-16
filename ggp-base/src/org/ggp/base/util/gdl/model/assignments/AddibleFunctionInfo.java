package org.ggp.base.util.gdl.model.assignments;

import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;

public interface AddibleFunctionInfo extends FunctionInfo {
	/**
	 * Convenience method, equivalent to addTuple.
	 */
	void addSentence(GdlSentence value);

	/**
	 * Adds a tuple to the known values for the sentence form.
	 * Adjusts the function info accordingly.
	 */
	void addTuple(List<GdlConstant> sentenceTuple);
}
