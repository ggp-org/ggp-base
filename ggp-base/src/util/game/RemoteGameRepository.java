package util.game;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.configuration.RemoteResourceLoader;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * Remote game repositories provide access to game resources stored on game
 * repository servers on the web. These require a network connection to work.
 * 
 * @author Sam
 */
public final class RemoteGameRepository extends GameRepository {
    private final String theRepoURL;
    
    public RemoteGameRepository(String theURL) {
        theRepoURL = theURL;
    }
    
    protected Set<String> getUncachedGameKeys() {
        try {
            Set<String> theGameKeys = new HashSet<String>();
            JSONArray theArray = RemoteResourceLoader.loadJSONArray(theRepoURL + "/games/");
            
            for(int i = 0; i < theArray.length(); i++) {
                theGameKeys.add(theArray.getString(i));
            }
            
            return theGameKeys;
        } catch (Exception e) {
            // TODO: Log this exception somewhere?
            return null;
        }
    }
    
    protected Game getUncachedGame(String theKey) {
        return loadSingleGame(getGameURL(theKey));
    }
    
    public static Game loadSingleGame(String theGameURL) {
        try {
            String[] theSplitURL = theGameURL.split("/");
            String theKey = theSplitURL[theSplitURL.length-1];
            
            JSONObject theMetadata = getGameMetadataFromRepository(theGameURL);
            
            String theName = null;
            try {
                theName = theMetadata.getString("gameName");
            } catch(JSONException e) {}
            
            String theDescription = getGameResourceFromMetadata(theGameURL, theMetadata, "description");                
            String theStylesheet = getGameResourceFromMetadata(theGameURL, theMetadata, "stylesheet");
            List<Gdl> theRules = getGameRulesheetFromMetadata(theGameURL, theMetadata);
            
            return new Game(theKey, theName, theDescription, theGameURL, theStylesheet, theRules);
        } catch(IOException e) {
            return null;
        }        
    }    
    
    // ============================================================================================
    private String getGameURL(String theGame) {
        return theRepoURL + "/games/" + theGame + "/";
    }
    
    private static JSONObject getGameMetadataFromRepository(String theGameURL) throws IOException {
        return RemoteResourceLoader.loadJSON(theGameURL);
    }
    
    private static String getGameResourceFromMetadata(String theGameURL, JSONObject theMetadata, String theResource) {
        try {
            String theResourceFile = theMetadata.getString(theResource);
            return RemoteResourceLoader.loadRaw(theGameURL + theResourceFile);
        } catch (Exception e) {
            return null;
        }
    } 
        
    private static List<Gdl> getGameRulesheetFromMetadata(String theGameURL, JSONObject theMetadata) {
        try {
            String theRulesheetFile = theMetadata.getString("rulesheet");
            return KifReader.readURL(theGameURL + theRulesheetFile);
        } catch (Exception e) {
            return null;
        }
    } 
}