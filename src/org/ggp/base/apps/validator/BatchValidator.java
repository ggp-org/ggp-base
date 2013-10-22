package org.ggp.base.apps.validator;

import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.validator.BasesInputsValidator;
import org.ggp.base.validator.OPNFValidator;
import org.ggp.base.validator.SimulationValidator;
import org.ggp.base.validator.StaticValidator;
import org.ggp.base.validator.GameValidator;
import org.ggp.base.validator.ValidatorException;

/**
 * BatchValidator does game validation on all of the games in a given game repository.
 * This allows you to quickly determine which games need to be repaired, given a large
 * existing game repository with games of varying quality.
 * 
 * @author schreib
 */
public final class BatchValidator
{
	public static void main(String[] args)
	{
		GameRepository repo = new CloudGameRepository("games.ggp.org/base");		
		for (String gameKey : repo.getGameKeys()) {
			if (gameKey.contains("amazons") || gameKey.contains("knightazons") || gameKey.contains("factoringImpossibleTurtleBrain") || gameKey.contains("quad") || gameKey.contains("blokbox") || gameKey.contains("othello"))
				continue;
			Game game = repo.getGame(gameKey);
			GameValidator[] theValidators = new GameValidator[] {
					new StaticValidator(),
					new BasesInputsValidator(3000),
					new SimulationValidator(300, 10),
					new OPNFValidator(),
			};
			System.out.print(gameKey + " ... ");
			System.out.flush();
			boolean isValid = true;
			for (GameValidator theValidator : theValidators) {
				try {
					theValidator.checkValidity(game);
				} catch (ValidatorException ve) {
					System.out.println("Failed: " + ve);
					isValid = false;
					break;
				}
			}
			if (isValid) {
				System.out.println("Passed!");
			}
		}		
	}
}