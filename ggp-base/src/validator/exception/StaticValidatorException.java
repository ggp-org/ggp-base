package validator.exception;

@SuppressWarnings("serial")
public class StaticValidatorException extends Exception {
	String explanation;
	protected StaticValidatorException() {
		
	}
	public StaticValidatorException(String explanation) {
		this.explanation = explanation;
	}
	
	@Override
	public String toString() {
		return explanation;
	}
}
