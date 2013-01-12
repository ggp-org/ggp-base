package org.ggp.base.validator.exception;

@SuppressWarnings("serial")
public class StaticValidatorException extends Exception {
	public StaticValidatorException(String explanation) {
		super("GDL validation error: " + explanation);
	}
}
