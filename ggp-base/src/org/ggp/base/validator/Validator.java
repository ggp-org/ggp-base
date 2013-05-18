package org.ggp.base.validator;

import org.ggp.base.util.game.Game;
import org.ggp.base.validator.exception.ValidatorException;

public interface Validator {
	public void checkValidity(Game theGame) throws ValidatorException;
}