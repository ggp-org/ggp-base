package org.ggp.base.validator;

import org.ggp.base.util.game.Game;

public interface Validator {
	public void checkValidity(Game theGame) throws ValidatorException;
}