package util.galaxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.gdl.grammar.Gdl;
import util.kif.KifReader;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * GalaxyRepositoriy is a prototype system for loading game resources from a
 * game repository server that adheres to the GGP Galaxy Fed0 protocol.
 * 
 * @author Sam
 */
public class GalaxyRepository {
    public static String[] getGamesFromRepository(String theURL) throws IOException {
        try {
            List<String> theGames = new ArrayList<String>();
            JSONArray theArray = RemoteResourceLoader.loadJSONArray(theURL);
            
            for(int i = 0; i < theArray.length(); i++) {
                theGames.add(theArray.getString(i));
            }
            
            String[] emptyArray = new String[0];
            return theGames.toArray(emptyArray);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }
    
    public static JSONObject getGameMetadataFromRepository(String theURL, String theGame) throws IOException {
        return RemoteResourceLoader.loadJSON(theURL + "/games/" + theGame + "/");
    }
    
    public static List<Gdl> getGameRulesheetFromRepository(String theURL, String theGame) {
        try {
            JSONObject theMetadata = getGameMetadataFromRepository(theURL, theGame);
            String theRulesheetFile = theMetadata.getString("rulesheet");
            return KifReader.readURL(theURL + "/games/" + theGame + "/" + theRulesheetFile);
        } catch (Exception e) {
            return null;
        }
    }
}