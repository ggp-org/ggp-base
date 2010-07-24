package validator.exception;

import util.gdl.grammar.Gdl;

@SuppressWarnings("serial")
public class InvalidGdlObjectException extends StaticValidatorException {
	private final Gdl object;
	
	public InvalidGdlObjectException(Gdl object) {
		this.object = object;
	}
	
	@Override
	public String toString() {
		return "The object " + object + " in the game description is neither a rule nor a relation";
	}
}
