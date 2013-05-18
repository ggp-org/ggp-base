package org.ggp.base.validator;

@SuppressWarnings("serial")
public class ValidatorException extends Exception {
	public ValidatorException(String explanation) {
		super("Validator: " + explanation);
	}
}
