package util.kif;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	public static List<Gdl> read(String filename) throws IOException, SymbolFormatException, GdlFormatException {
	    return convertIntoGdl("(" + readFile(filename) + ")");
	}
	
    public static List<Gdl> readStream(InputStream in) throws IOException, SymbolFormatException, GdlFormatException {
        return convertIntoGdl("(" + readStream(new BufferedReader(new InputStreamReader(in))) + ")");
    }	
	
	protected static List<Gdl> convertIntoGdl(String gameDescription) throws IOException, SymbolFormatException, GdlFormatException {
        List<Gdl> contents = new ArrayList<Gdl>();
        
        SymbolList list = (SymbolList) SymbolFactory.create(gameDescription);
        for (int i = 0; i < list.size(); i++)
        {
        contents.add(GdlFactory.create(list.get(i)));
        }
        
        return contents;
	}

	protected static String readFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		return readStream(br);
	}
	
	protected static String readStream(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = null;

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