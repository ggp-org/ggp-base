package org.ggp.base.validator;

public class ValidatorWarning {
	private final String warningMessage;

	public ValidatorWarning(String warningMessage) {
		this.warningMessage = warningMessage;
	}

	@Override
	public String toString() {
		return warningMessage;
	}
}
