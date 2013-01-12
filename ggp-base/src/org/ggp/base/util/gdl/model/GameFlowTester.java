package org.ggp.base.util.gdl.model;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;

public class GameFlowTester {
	//This doesn't really "test" the game flow so much as let us
	//examine it to evaluate it.
	public static void main(String[] args) throws InterruptedException {
		String gameName = "tictactoe";		
	    Game theGame = GameRepository.getDefaultRepository().getGame(gameName);
		GameFlow flow = new GameFlow(theGame.getRules());

		System.out.println("Size of flow: " + flow.getNumTurns());
		System.out.println("Sentence forms in flow: " + flow.getSentenceForms());
		for(int i = 0; i < flow.getNumTurns(); i++) {
			System.out.println("On turn " + i + ": " + flow.getSentencesTrueOnTurn(i));
		}
		System.out.println("Turn after last: " + flow.getTurnAfterLast());
	}
}
