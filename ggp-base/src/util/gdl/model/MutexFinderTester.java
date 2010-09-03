package util.gdl.model;

import java.io.IOException;
import java.util.List;

import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import util.symbol.factory.exceptions.SymbolFormatException;

public class MutexFinderTester {

	//This is to examine mutexes, not to automatically verify them.
	public static void main(String[] args) {
		String gameName = "brawl";
		
			try {
				List<Gdl> description = KifReader.read("games/rulesheets/" + gameName + ".kif");
				
				System.out.println(MoveMutexFinder.findMutexes(description));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SymbolFormatException e) {
				e.printStackTrace();
			} catch (GdlFormatException e) {
				e.printStackTrace();
			}
			
		
	}

}
