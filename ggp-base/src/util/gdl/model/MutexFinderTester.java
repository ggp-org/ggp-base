package util.gdl.model;

import util.game.Game;
import util.game.GameRepository;

public class MutexFinderTester {
	//This is to examine mutexes, not to automatically verify them.
	public static void main(String[] args) throws InterruptedException {
		String gameName = "brawl";
		Game theGame = GameRepository.getDefaultRepository().getGame(gameName);
        System.out.println(MoveMutexFinder.findMutexes(theGame.getRules()));
	}
}
