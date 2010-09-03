package util.gdl.model;

import java.io.IOException;
import java.util.List;

import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import util.symbol.factory.exceptions.SymbolFormatException;

public class GameFlowTester {

	//This doesn't really "test" the game flow so much as let us
	//examine it to evaluate it.
	public static void main(String[] args) {
		String gameName = "tictactoe";
		
		try {
			List<Gdl> description = KifReader.read("games/rulesheets/" + gameName + ".kif");

			GameFlow flow = new GameFlow(description);

			System.out.println("Size of flow: " + flow.getNumTurns());
			System.out.println("Sentence forms in flow: " + flow.getSentenceForms());
			for(int i = 0; i < flow.getNumTurns(); i++) {
				System.out.println("On turn " + i + ": " + flow.getSentencesTrueOnTurn(i));
			}
			System.out.println("Turn after last: " + flow.getTurnAfterLast());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SymbolFormatException e) {
			e.printStackTrace();
		} catch (GdlFormatException e) {
			e.printStackTrace();
		}
	}

}
