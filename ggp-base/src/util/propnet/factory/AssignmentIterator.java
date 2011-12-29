package util.propnet.factory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlVariable;

public interface AssignmentIterator extends Iterator<Map<GdlVariable, GdlConstant>> {

	/**
	 * Request that the next assignment change at least one
	 * of the listed variables from its current assignment.
	 */
	void changeOneInNext(Collection<GdlVariable> varsToChange,
			Map<GdlVariable, GdlConstant> assignment);

}
