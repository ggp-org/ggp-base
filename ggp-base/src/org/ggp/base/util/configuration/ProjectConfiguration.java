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

package org.ggp.base.util.configuration;

import java.io.File;

public class ProjectConfiguration {
    /* Game rulesheet repository information */
    private static final String gamesRootDirectoryPath = "games";
    
    public static final File gameImagesDirectory = new File(new File(gamesRootDirectoryPath, "resources"), "images");
    public static final File gameCacheDirectory = new File(gamesRootDirectoryPath, "cache");
    
    /* Class object file information */
    public static final String[] classRoots = new String[] {"bin"};    
}