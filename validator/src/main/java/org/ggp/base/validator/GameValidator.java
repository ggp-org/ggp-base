package org.ggp.base.validator;

import java.util.List;

import org.ggp.base.util.game.Game;

public interface GameValidator {
    public List<ValidatorWarning> checkValidity(Game theGame) throws ValidatorException;
}