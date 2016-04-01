package org.ggp.base.util.gdl.model.assignments;

import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlVariable;


//TODO: Get rid of this class in some way...
//Or, just remake into AssignmentIterationPlan
public interface Assignments extends Iterable<Map<GdlVariable, GdlConstant>> {

    AssignmentIterator getIterator();

}
