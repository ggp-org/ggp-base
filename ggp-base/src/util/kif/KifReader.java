package util.kif;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.gdl.factory.GdlFactory;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.symbol.factory.SymbolFactory;
import util.symbol.factory.exceptions.SymbolFormatException;
import util.symbol.grammar.SymbolList;

public class KifReader
{
	public static List<Gdl> read(String filename) throws IOException, SymbolFormatException, GdlFormatException
	{
		List<Gdl> contents = new ArrayList<Gdl>();

		String string = "(" + readFile(filename) + ")";
		SymbolList list = (SymbolList) SymbolFactory.create(string);
		for (int i = 0; i < list.size(); i++)
		{
			contents.add(GdlFactory.create(list.get(i)));
		}

		return contents;
	}

	protected static String readFile(String filename) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		String line = null;

		BufferedReader br = new BufferedReader(new FileReader(filename));
		while ((line = br.readLine()) != null)
		{
			int comment = line.indexOf(';');
			int cutoff = (comment == -1) ? line.length() : comment;

			sb.append(line.substring(0, cutoff));
			sb.append(" ");
		}

		return sb.toString();
	}
}