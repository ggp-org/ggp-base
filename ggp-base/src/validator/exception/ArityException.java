package validator.exception;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlSentence;

@SuppressWarnings("serial")
public final class ArityException extends StaticValidatorException {
	private final Gdl object;
	private final GdlConstant name;
	private final int arity1, arity2;

	public ArityException(GdlFunction function, Integer curArity) {
		object = function;
		name = function.getName();
		arity1 = function.arity();
		arity2 = curArity;
	}
	
	public ArityException(GdlSentence sentence, Integer curArity) {
		object = sentence;
		name = sentence.getName();
		arity1 = sentence.arity();
		arity2 = curArity;
	}

	@Override
	public String toString() {
		return object.getClass().getSimpleName() + " with name " + name + " has two different arities: " + arity1 + " and " + arity2;
	}

}
