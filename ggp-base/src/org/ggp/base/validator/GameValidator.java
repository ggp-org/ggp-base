package org.ggp.base.validator;

import org.ggp.base.util.game.Game;

public interface GameValidator {
	public void checkValidity(Game theGame) throws ValidatorException;
}