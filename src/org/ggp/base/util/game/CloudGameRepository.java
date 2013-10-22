package org.ggp.base.util.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import external.JSON.JSONObject;

/**
 * Cloud game repositories provide access to game resources stored on game
 * repository servers on the web, while continuing to work while the user is
 * offline through aggressive caching based on the immutability + versioning
 * scheme provided by the repository servers.
 * 
 * Essentially, each game has a version number stored in the game metadata
 * file. Game resources are immutable until this version number changes, at
 * which point the game needs to be reloaded. Version numbers are passed along
 * and stored in the match descriptions, and repository servers will continue
 * to serve old versions when specifically requested, so it is valid to use any
 * historical game version when generating a match -- this is why we don't need
 * to worry about our offline cache becoming stale/invalid. However, to stay up
 * to date with the latest bugfixes, etc, we aggressively refresh the cache any
 * time we can connect to the repository server, as a matter of policy.
 * 
 * Cached games are stored locally, in a directory managed by this class. These
 * files are compressed, to decrease their footprint on the local disk. GGP Base
 * has its SVN rules set up so that these caches are ignored by SVN. 
 * 
 * @author Sam
 */
public final class CloudGameRepository extends GameRepository {
    private final String theRepoURL;
    private final File theCacheDirectory;
    private static boolean needsRefresh = true;
    
    public CloudGameRepository(String theURL) {
        theRepoURL = RemoteGameRepository.properlyFormatURL(theURL);
        
        // Generate a unique hash of the repository URL, to use as the
        // local directory for files for the offline cache.
        StringBuilder theCacheHash = new StringBuilder();
        try {
            byte[] bytesOfMessage = theRepoURL.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] theDigest = md.digest(bytesOfMessage);            
            for(int i = 0; i < theDigest.length; i++) {
                theCacheHash.append(Math.abs(theDigest[i]));
            }            
        } catch(Exception e) {
            theCacheHash = null;
        }

    	File theCachesDirectory = new File(System.getProperty("user.home"), ".ggpserver-gamecache");
    	theCachesDirectory.mkdir();
    	theCacheDirectory = new File(theCachesDirectory, "repoHash" + theCacheHash);
    	if (theCacheDirectory.exists()) {
    		// For existing caches, only force a full refresh at most once per day
            needsRefresh = (System.currentTimeMillis() - theCacheDirectory.lastModified()) > 86400000;    		
    	} else {    		
    		theCacheDirectory.mkdir();
    		needsRefresh = true;
    	}

        if (needsRefresh) {
        	Thread refreshThread = new RefreshCacheThread(theRepoURL);
        	refreshThread.start();        	
        	// Update the game cache asynchronously if there are already games.
        	// Otherwise, force a blocking update.
        	if (theCacheDirectory.listFiles().length == 0) {
        		try {
        			refreshThread.join();
        		} catch (InterruptedException e) {
        			;
        		}
        	}
        	theCacheDirectory.setLastModified(System.currentTimeMillis());
        	needsRefresh = false;
        }
    }
    
    protected Set<String> getUncachedGameKeys() {
        Set<String> theKeys = new HashSet<String>();
        for(File game : theCacheDirectory.listFiles()) {
            theKeys.add(game.getName().replace(".zip", ""));
        }
        return theKeys;
    }
    
    protected Game getUncachedGame(String theKey) {
        Game cachedGame = loadGameFromCache(theKey);
        if (cachedGame != null) {
        	return cachedGame;
        }
        // Request the game directly on a cache miss.
        return new RemoteGameRepository(theRepoURL).getGame(theKey);
    }

    // ================================================================
    
    // Games are cached asynchronously in their own threads.
    class RefreshCacheForGameThread extends Thread {
        RemoteGameRepository theRepository;
        String theKey;
        
        public RefreshCacheForGameThread(RemoteGameRepository a, String b) {
            theRepository = a;
            theKey = b;
        }
        
        @Override
        public void run() {
            try {
                String theGameURL = theRepository.getGameURL(theKey);
                JSONObject theMetadata = RemoteGameRepository.getGameMetadataFromRepository(theGameURL);

                int repoVersion = theMetadata.getInt("version");
                String versionedRepoURL = RemoteGameRepository.addVersionToGameURL(theGameURL, repoVersion);

                Game myGameVersion = loadGameFromCache(theKey);
                String myVersionedRepoURL = "";
                if (myGameVersion != null)
                    myVersionedRepoURL = myGameVersion.getRepositoryURL();

                if (!versionedRepoURL.equals(myVersionedRepoURL)) {
                    // Cache miss: we don't have the current version for
                    // this game, and so we need to load it from the web.
                    Game theGame = RemoteGameRepository.loadSingleGameFromMetadata(theKey, theGameURL, theMetadata);
                    saveGameToCache(theKey, theGame);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    class RefreshCacheThread extends Thread {
        String theRepoURL;
        
        public RefreshCacheThread(String theRepoURL) {
            this.theRepoURL = theRepoURL;
        }
        
        @Override
        public void run() {            
            try {
                // Sleep for the first two seconds after which the cache is loaded,
                // so that we don't interfere with the user interface startup.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            RemoteGameRepository remoteRepository = new RemoteGameRepository(theRepoURL);

            System.out.println("Updating the game cache...");
            long beginTime = System.currentTimeMillis();            

            // Since games are immutable, we can guarantee that the games listed
            // by the repository server includes the games in the local cache, so
            // we can be happy just updating/refreshing the listed games.
            Set<String> theGameKeys = remoteRepository.getGameKeys();
            if (theGameKeys == null) return;
            
            // If the server offers a single combined metadata file, download that
            // and use it to avoid checking games that haven't gotten new versions.
            JSONObject bundledMetadata = remoteRepository.getBundledMetadata();
            if (bundledMetadata != null) {
                Set<String> unchangedKeys = new HashSet<String>();
                for (String theKey : theGameKeys) {
                    try {                    
                        Game myGameVersion = loadGameFromCache(theKey);
                        if (myGameVersion == null)
                            continue;                    
                    
                        String remoteGameURL = remoteRepository.getGameURL(theKey);
                        int remoteVersion = bundledMetadata.getJSONObject(theKey).getInt("version");
                        String remoteVersionedGameURL = RemoteGameRepository.addVersionToGameURL(remoteGameURL, remoteVersion);
                    
                        if (myGameVersion.getRepositoryURL().equals(remoteVersionedGameURL)) {
                            unchangedKeys.add(theKey);
                        }
                    } catch (Exception e) {
                        continue;
                    }                        
                }
                theGameKeys.removeAll(unchangedKeys);
            }

            // Start threads to update every entry in the cache (or at least verify
            // that the entry doesn't need to be updated).
            Set<Thread> theThreads = new HashSet<Thread>();
            for (String gameKey : theGameKeys) {
                Thread t = new RefreshCacheForGameThread(remoteRepository, gameKey);
                t.start();
                theThreads.add(t);
            }

            // Wait until we've updated the cache before continuing.
            for (Thread t : theThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    ;
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Updating the game cache took: " + (endTime - beginTime) + "ms.");              
        }
    }    
    
    // ================================================================    

    private synchronized void saveGameToCache(String theKey, Game theGame) {
        if (theGame == null) return;
        
        File theGameFile = new File(theCacheDirectory, theKey + ".zip");
        try {
            theGameFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(theGameFile);
            GZIPOutputStream gOut = new GZIPOutputStream(fOut);
            PrintWriter pw = new PrintWriter(gOut);
            pw.print(theGame.serializeToJSON());
            pw.flush();
            pw.close();
            gOut.close();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();            
        }
    }
    
    private synchronized Game loadGameFromCache(String theKey) {
        File theGameFile = new File(theCacheDirectory, theKey + ".zip");        
        String theLine = null;
        try {
            FileInputStream fIn = new FileInputStream(theGameFile);
            GZIPInputStream gIn = new GZIPInputStream(fIn);
            InputStreamReader ir = new InputStreamReader(gIn);
            BufferedReader br = new BufferedReader(ir);
            theLine = br.readLine();
            br.close();
            ir.close();
            gIn.close();
            fIn.close();
        } catch (Exception e) {
            ;
        }
        
        if (theLine == null) return null;
        return Game.loadFromJSON(theLine);
    }

    // ================================================================
    
    public static void main(String[] args) {
        GameRepository theRepository = new CloudGameRepository("games.ggp.org/base");

        long beginTime = System.currentTimeMillis();

        Map<String, Game> theGames = new HashMap<String, Game>();
        for(String gameKey : theRepository.getGameKeys()) {
            theGames.put(gameKey, theRepository.getGame(gameKey));            
        }
        System.out.println("Games: " + theGames.size());
        
        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - beginTime) + "ms.");
    }
}