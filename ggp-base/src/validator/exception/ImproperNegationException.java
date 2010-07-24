package validator.exception;

import util.gdl.grammar.GdlNot;

@SuppressWarnings("serial")
public final class ImproperNegationException extends StaticValidatorException {
	private final GdlNot not;
	
	public ImproperNegationException(GdlNot not) {
		this.not = not;
	}
	
	@Override
	public String toString() {
		return "The GdlNot object " + not + " contains a non-sentence literal";
	}
}
