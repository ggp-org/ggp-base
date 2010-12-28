package util.game;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * Since network connects are potentially expensive, this class will cache any
 * game resources in memory after they are loaded.
 * 
 * @author Sam
 */
public final class RemoteGameRepository implements GameRepository {
    private final String theRepoURL;
    
    // Caches, which are lazily filled.
    private Set<String> theGameKeys;
    private Map<String, Game> theGames;
    
    public RemoteGameRepository(String theURL) {
        theRepoURL = theURL;
        theGames = new HashMap<String, Game>();
    }
    
    public Set<String> getGameKeys() {
        if (theGameKeys == null)
            theGameKeys = getGameKeysFromRepository();
        return theGameKeys;
    }
    
    public Game getGame(String theKey) {
        if (!theGames.containsKey(theKey)) {
            try {
                JSONObject theMetadata = getGameMetadataFromRepository(theKey);
                
                String theName = null;
                try {
                    theName = theMetadata.getString("name");
                } catch(JSONException e) {}
                String theRepositoryURL = theRepoURL + "/games/" + theKey + "/";
                String theDescription = getGameResourceFromMetadata(theKey, theMetadata, "description");                
                String theStylesheet = getGameResourceFromMetadata(theKey, theMetadata, "stylesheet");
                List<Gdl> theRules = getGameRulesheetFromMetadata(theKey, theMetadata);
                
                Game theGame = new Game(theKey, theName, theRepositoryURL, theDescription, theStylesheet, theRules);
                theGames.put(theKey, theGame);
            } catch(IOException e) {
                // TODO: Log this exception somewhere?
            }
        }
        return theGames.get(theKey);
    }
    
    // ============================================================================================
    public Set<String> getGameKeysFromRepository() {
        try {
            Set<String> theGameKeys = new HashSet<String>();
            JSONArray theArray = RemoteResourceLoader.loadJSONArray(theRepoURL + "/games/");
            
            for(int i = 0; i < theArray.length(); i++) {
                theGameKeys.add(theArray.getString(i));
            }
            
            return theGameKeys;
        } catch (Exception e) {
            System.err.println(e);
            // TODO: Log this exception somewhere?
            return null;
        }
    }
    
    public JSONObject getGameMetadataFromRepository(String theGame) throws IOException {
        return RemoteResourceLoader.loadJSON(theRepoURL + "/games/" + theGame + "/");
    }
    
    public String getGameResourceFromMetadata(String theGame, JSONObject theMetadata, String theResource) {
        try {
            String theResourceFile = theMetadata.getString(theResource);
            return RemoteResourceLoader.loadRaw(theRepoURL + "/games/" + theGame + "/" + theResourceFile);
        } catch (Exception e) {
            return null;
        }
    } 
        
    public List<Gdl> getGameRulesheetFromMetadata(String theGame, JSONObject theMetadata) {
        try {
            String theRulesheetFile = theMetadata.getString("rulesheet");
            return KifReader.readURL(theRepoURL + "/games/" + theGame + "/" + theRulesheetFile);
        } catch (Exception e) {
            return null;
        }
    } 
}