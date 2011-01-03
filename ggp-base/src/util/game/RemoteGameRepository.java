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
        try {
            JSONObject theMetadata = getGameMetadataFromRepository(theKey);
            
            String theName = null;
            try {
                theName = theMetadata.getString("gameName");
            } catch(JSONException e) {}
            String theRepositoryURL = theRepoURL + "/games/" + theKey + "/";
            String theDescription = getGameResourceFromMetadata(theKey, theMetadata, "description");                
            String theStylesheet = getGameResourceFromMetadata(theKey, theMetadata, "stylesheet");
            List<Gdl> theRules = getGameRulesheetFromMetadata(theKey, theMetadata);
            
            return new Game(theKey, theName, theDescription, theRepositoryURL, theStylesheet, theRules);
        } catch(IOException e) {
            return null;
        }
    }
    
    // ============================================================================================
    private JSONObject getGameMetadataFromRepository(String theGame) throws IOException {
        return RemoteResourceLoader.loadJSON(theRepoURL + "/games/" + theGame + "/");
    }
    
    private String getGameResourceFromMetadata(String theGame, JSONObject theMetadata, String theResource) {
        try {
            String theResourceFile = theMetadata.getString(theResource);
            return RemoteResourceLoader.loadRaw(theRepoURL + "/games/" + theGame + "/" + theResourceFile);
        } catch (Exception e) {
            return null;
        }
    } 
        
    private List<Gdl> getGameRulesheetFromMetadata(String theGame, JSONObject theMetadata) {
        try {
            String theRulesheetFile = theMetadata.getString("rulesheet");
            return KifReader.readURL(theRepoURL + "/games/" + theGame + "/" + theRulesheetFile);
        } catch (Exception e) {
            return null;
        }
    } 
}