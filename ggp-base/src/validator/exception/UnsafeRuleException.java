package validator.exception;

import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlVariable;

@SuppressWarnings("serial")
public class UnsafeRuleException extends StaticValidatorException {
	private final GdlRule rule;
	private final GdlVariable var;
	
	public UnsafeRuleException(GdlRule rule, GdlVariable var) {
		this.rule = rule;
		this.var = var;
	}
	
	@Override
	public String toString() {
		return "Unsafe rule " + rule + ": Variable " + var + " is not defined in a positive relation in the body";
	}

}
