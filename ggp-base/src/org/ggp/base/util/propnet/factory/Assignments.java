package org.ggp.base.util.propnet.factory;

import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlVariable;



public interface Assignments extends Iterable<Map<GdlVariable, GdlConstant>> {

	AssignmentIterator getIterator();

}
