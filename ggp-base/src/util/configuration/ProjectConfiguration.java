/**
 *  ProjectConfiguration handles the project-specific directory settings.
 *  This class stores the paths of the game directory and the class binary
 *  directories, so they can quickly be changed and overridden.
 *  
 *  For example, if you create a second project in Eclipse which includes
 *  this "GGP Base" project, you can override "gamesRootDirectoryPath" to
 *  be "..\\GGP Base\\games" in the new project, and add "..\\GGP Base\\bin"
 *  to the classRoots array, and then your new project will automagically be
 *  able to find all of the games, and all of the player objects stored both
 *  in your new project, and in the GGP Base project.
 * 
 * @author Sam Schreiber
 */

package util.configuration;

import java.io.File;

public class ProjectConfiguration {
    /* Game rulesheet repository information */
    private static final String gamesRootDirectoryPath = "games";
    
    public static final File gamesRootDirectory = new File(gamesRootDirectoryPath);
    public static final File gameImagesDirectory = new File(gamesRootDirectoryPath, "images");
    public static final File gameRulesheetsDirectory = new File(gamesRootDirectoryPath, "rulesheets");
    public static final File gameStylesheetsDirectory = new File(gamesRootDirectoryPath, "stylesheets");    
    
    public static final String gamesRootPath = gamesRootDirectory.getAbsolutePath() + File.separatorChar;
    public static final String gameImagesPath = gameImagesDirectory.getAbsolutePath() + File.separatorChar;
    public static final String gameRulesheetsPath = gameRulesheetsDirectory.getAbsolutePath() + File.separatorChar;
    public static final String gameStylesheetsPath = gameStylesheetsDirectory.getAbsolutePath() + File.separatorChar;
    
    public static void main(String[] args) {
        System.out.println("Games root directory: " + gamesRootPath);
        System.out.println("Games images directory: " + gameImagesPath);
        System.out.println("Games rulesheets directory: " + gameRulesheetsPath);
        System.out.println("Games stylesheets directory: " + gameStylesheetsPath);
    }
    
    /* Cached serialized propnet information */
    private static final String propNetCacheDirectoryPath = "propnet_cache";
    public static final File propNetCacheDirectory = new File(propNetCacheDirectoryPath);
    
    /* Class object file information */
    public static final String[] classRoots = new String[] {"bin"};    
}