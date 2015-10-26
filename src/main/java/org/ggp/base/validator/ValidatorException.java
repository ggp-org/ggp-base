package org.ggp.base.validator;

@SuppressWarnings("serial")
public class ValidatorException extends Exception {
    public ValidatorException(String explanation) {
        super("Validator: " + explanation);
    }

    public ValidatorException(String explanation, Throwable t) {
        super("Validator: " + explanation, t);
    }
}
