package util.kif;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import util.gdl.factory.GdlFactory;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.symbol.factory.SymbolFactory;
import util.symbol.factory.exceptions.SymbolFormatException;
import util.symbol.grammar.SymbolList;

/**
 * KifReader is responsible for loading files which are written in the
 * Knowledge Interchange Format (KIF). Specifically, we are interested in
 * loading game rulesheets, which are written in GDL, which is designed as
 * a strict subset of KIF.
 * 
 * Given a resource identifier (a filename, a stream, or a URL), this class
 * can load a game rulesheet from that resource, returning the GDL objects
 * defined in the rulesheet.
 * 
 * More details at: http://www-ksl.stanford.edu/knowledge-sharing/kif/
 */
public class KifReader
{
	public static List<Gdl> read(String filename) throws IOException, SymbolFormatException, GdlFormatException {
	    return convertIntoGdl("(" + readStream(new BufferedReader(new FileReader(filename))) + ")");
	}
	
    public static List<Gdl> readStream(InputStream in) throws IOException, SymbolFormatException, GdlFormatException {
        return convertIntoGdl("(" + readStream(new BufferedReader(new InputStreamReader(in))) + ")");
    }
    
    public static List<Gdl> readURL(String theURL) throws IOException, SymbolFormatException, GdlFormatException {
        URL url = new URL(theURL);
        URLConnection urlConnection = url.openConnection();                
        if (urlConnection.getContentLength() == 0)
            throw new IOException("Could not load URL: " + theURL);
        return convertIntoGdl("(" + readStream(new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) + ")");
    }

    /* Actually read the game rulesheet and convert it into GDL */
	protected static List<Gdl> convertIntoGdl(String gameDescription) throws SymbolFormatException, GdlFormatException {
        List<Gdl> contents = new ArrayList<Gdl>();
        
        SymbolList list = (SymbolList) SymbolFactory.create(gameDescription);
        for (int i = 0; i < list.size(); i++)
        {
            contents.add(GdlFactory.create(list.get(i)));
        }
        
        return contents;
	}

	/* Given a stream, parse out all the comments and aggregate it */
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