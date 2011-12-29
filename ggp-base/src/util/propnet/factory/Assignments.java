package util.propnet.factory;

import java.util.Map;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlVariable;


public interface Assignments extends Iterable<Map<GdlVariable, GdlConstant>> {

	AssignmentIterator getIterator();

}
