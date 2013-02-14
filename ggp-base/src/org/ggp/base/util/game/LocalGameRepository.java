package org.ggp.base.util.game;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.statemachine.Role;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import external.JSON.*;

/**
 * Local game repositories provide access to game resources stored on the
 * local disk, bundled with the GGP Base project. For consistency with the
 * web-based GGP.org infrastructure, this starts a simple HTTP server that
 * provides access to the local game resources, and then uses the standard
 * RemoteGameRepository interface to read from that server.
 * 
 * @author Sam
 */
public final class LocalGameRepository extends GameRepository {
    private static final int REPO_SERVER_PORT = 9140;
    private static HttpServer theLocalRepoServer = null;
    private static String theLocalRepoURL = "http://127.0.0.1:" + REPO_SERVER_PORT;
    
    private static RemoteGameRepository theRealRepo;
    
    public LocalGameRepository() {
        if (theLocalRepoServer == null) {
            try {
                theLocalRepoServer = HttpServer.create(new InetSocketAddress(REPO_SERVER_PORT), 0);
                theLocalRepoServer.createContext("/", new LocalRepoServer());
                theLocalRepoServer.setExecutor(null); // creates a default executor
                theLocalRepoServer.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        theRealRepo = new RemoteGameRepository(theLocalRepoURL);
    }
    
    @Override
    protected Game getUncachedGame(String theKey) {
        return theRealRepo.getGame(theKey);
    }

    @Override
    protected Set<String> getUncachedGameKeys() {
        return theRealRepo.getGameKeys();
    }
    
    // ========================
    
    class LocalRepoServer implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String theURI = t.getRequestURI().toString();
            byte[] response = BaseRepository.getResponseBytesForURI(theURI);
            if (response == null) {
                t.sendResponseHeaders(404, 0);
                OutputStream os = t.getResponseBody();
                os.close();                
            } else {
                t.sendResponseHeaders(200, response.length);
                OutputStream os = t.getResponseBody();
                os.write(response);
                os.close();
            }
        }
    }    
    
    static class BaseRepository {
        public static final String repositoryRootDirectory = theLocalRepoURL;    
        
        public static byte[] getResponseBytesForURI(String reqURI) throws IOException {
            // Files not under /games/games/ aren't versioned,
            // and can just be accessed directly.
            if (!reqURI.startsWith("/games/")) {
                return getBytesForFile(new File("games" + reqURI));
            }
            
            // Provide a listing of all of the metadata files for all of
            // the games, on request.
            if (reqURI.equals("/games/metadata")) {
                JSONObject theGameMetaMap = new JSONObject();
                for (String gameName : new File("games", "games").list()) {
                    if (gameName.equals(".svn")) continue;
                    try {
                        theGameMetaMap.put(gameName, new JSONObject(new String(getResponseBytesForURI("/games/" + gameName + "/"))));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return theGameMetaMap.toString().getBytes();
            }
            
            // Accessing the folder containing a game should show the game's
            // associated metadata (which includes the contents of the folder).
            if(reqURI.endsWith("/") && reqURI.length() > 9) {
                reqURI += "METADATA";
            }
            
            // Extract out the version number
            String thePrefix = reqURI.substring(0, reqURI.lastIndexOf("/"));
            String theSuffix = reqURI.substring(reqURI.lastIndexOf("/")+1);
            Integer theExplicitVersion = null;
            try {
                String vPart = thePrefix.substring(thePrefix.lastIndexOf("/v")+2);
                theExplicitVersion = Integer.parseInt(vPart);            
                thePrefix = thePrefix.substring(0, thePrefix.lastIndexOf("/v"));
            } catch (Exception e) {
                ;
            }
            
            // Sanity check: raise an exception if the parsing didn't work.
            if (theExplicitVersion == null) {
                if (!reqURI.equals(thePrefix + "/" + theSuffix)) {
                    throw new RuntimeException(reqURI + " != [~v] " + (thePrefix + "/" + theSuffix));
                }
            } else {
                if (!reqURI.equals(thePrefix + "/v" + theExplicitVersion + "/" + theSuffix)) {
                    throw new RuntimeException(reqURI + " != [v] " + (thePrefix + "/v" + theExplicitVersion + "/" + theSuffix));
                }
            }

            // When no version number is explicitly specified, assume that we want the
            // latest version, whatever that is. Also, make sure the game version being
            // requested actually exists (i.e. is between 0 and the max version).
            int nMaxVersion = getMaxVersionForDirectory(new File("games", thePrefix));        
            Integer theFetchedVersion = theExplicitVersion;
            if (theFetchedVersion == null) theFetchedVersion = nMaxVersion;
            if (theFetchedVersion < 0 || theFetchedVersion > nMaxVersion) return null;

            while (theFetchedVersion >= 0) {
                byte[] theBytes = getBytesForVersionedFile(thePrefix, theFetchedVersion, theSuffix);
                if (theBytes != null) {
                    if (theSuffix.equals("METADATA")) {
                        theBytes = adjustMetadataJSON(reqURI, theBytes, theExplicitVersion, nMaxVersion);
                    }
                    return theBytes;
                }
                theFetchedVersion--;
            }
            return null;
        }
        
        // When the user requests a particular version, the metadata should always be for that version.
        // When the user requests the latest version, the metadata should always indicate the most recent version.
        public static byte[] adjustMetadataJSON(String reqURI, byte[] theMetaBytes, Integer nExplicitVersion, int nMaxVersion) throws IOException {
            try {
                JSONObject theMetaJSON = new JSONObject(new String(theMetaBytes));
                if (nExplicitVersion == null) {
                    theMetaJSON.put("version", nMaxVersion);             
                } else {
                    theMetaJSON.put("version", nExplicitVersion);
                }
                String theRulesheet = new String(getResponseBytesForURI(reqURI.replace("METADATA",theMetaJSON.getString("rulesheet"))));
                MetadataCompleter.completeMetadataFromRulesheet(theMetaJSON, theRulesheet);
                return theMetaJSON.toString().getBytes();
            } catch (JSONException je) {
                throw new IOException(je);
            }
        }    
        
        private static int getMaxVersionForDirectory(File theDir) {
            if (!theDir.exists() || !theDir.isDirectory()) {
                return -1;
            }
            
            int maxVersion = 0;
            String[] children = theDir.list();
            for (String s : children) {
                if (s.equals(".svn")) continue;
                if (s.startsWith("v")) {
                    int nVersion = Integer.parseInt(s.substring(1));
                    if (nVersion > maxVersion) {
                        maxVersion = nVersion;
                    }
                }
            }
            return maxVersion;
        }
        
        private static byte[] getBytesForVersionedFile(String thePrefix, int theVersion, String theSuffix) {
            if (theVersion == 0) {
                return getBytesForFile(new File("games", thePrefix + "/" + theSuffix));
            } else {
                return getBytesForFile(new File("games", thePrefix + "/v" + theVersion + "/" + theSuffix));
            }
        }
        
        private static byte[] getBytesForFile(File theFile) {
            try {
                if (!theFile.exists()) {
                    return null;
                } else if (theFile.isDirectory()) {
                    return readDirectory(theFile).getBytes();
                } else if (theFile.getName().endsWith(".png")) {
                    // TODO: Handle other binary formats?
                    return readBinaryFile(theFile);
                } else if (theFile.getName().endsWith(".xsl")) {
                    return transformXSL(readFile(theFile)).getBytes();
                } else if (theFile.getName().endsWith(".js")) {
                    return transformJS(readFile(theFile)).getBytes();
                } else {
                    return readFile(theFile).getBytes();
                }
            } catch (IOException e) {
                return null;
            }
        }
        
        private static String transformXSL(String theContent) {
            // Special case override for XSLT        
            return "<!DOCTYPE stylesheet [<!ENTITY ROOT \""+repositoryRootDirectory+"\">]>\n\n" + theContent;
        }
        
        private static String transformJS(String theContent) throws IOException {
            // Horrible hack; fix this later. Right now this is used to
            // let games share a common board user interface, but this should
            // really be handled in a cleaner, more general way with javascript
            // libraries and imports.
            if (theContent.contains("[BOARD_INTERFACE_JS]")) {
                String theCommonBoardJS = readFile(new File("games/resources/scripts/BoardInterface.js"));
                theContent = theContent.replaceFirst("\\[BOARD_INTERFACE_JS\\]", theCommonBoardJS);
            }
            return theContent;
        }

        private static String readDirectory(File theDirectory) throws IOException {
            StringBuilder response = new StringBuilder();
            // Show contents of the directory, using JSON notation.
            response.append("[");

            String[] children = theDirectory.list();
            for (int i=0; i<children.length; i++) {
            	if (children[i].equals(".svn")) continue;
                // Get filename of file or directory
                response.append("\"");
                response.append(children[i]);
                response.append("\",\n ");
            }

            response.delete(response.length()-3, response.length());
            response.append("]");
            return response.toString();
        }
        
        public static String readFile(File rootFile) throws IOException {
            // Show contents of the file.                                        
            FileReader fr = new FileReader(rootFile);
            BufferedReader br = new BufferedReader(fr);
        	try {
        		String response = "";
        		String line;
        		while( (line = br.readLine()) != null ) {
        			response += line + "\n";
        		}

        		return response;
        	} finally {
            	br.close();
            }
        }
        
        private static byte[] readBinaryFile(File rootFile) throws IOException {
            InputStream in = new FileInputStream(rootFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            while (in.read(buf) > 0) {
                out.write(buf);
            }
            in.close();
            
            return out.toByteArray();
        }
    }
    
    static class MetadataCompleter {
    	/**
    	 * Complete fields in the metadata procedurally, based on the game rulesheet.
    	 * This is used to fill in the number of roles, and create a list containing
    	 * the names of all of the roles. Applications which read the game metadata
    	 * can use these without also having to process the rulesheet.
    	 * 
    	 * @param theMetaJSON
    	 * @param theRulesheet
    	 * @throws JSONException
    	 */
    	public static void completeMetadataFromRulesheet(JSONObject theMetaJSON, String theRulesheet) throws JSONException {
    		List<Role> theRoles = Role.computeRoles(Game.createEphemeralGame(Game.preprocessRulesheet(theRulesheet)).getRules());
            theMetaJSON.put("roleNames", theRoles);
            theMetaJSON.put("numRoles", theRoles.size());
    	}    	
    }
}