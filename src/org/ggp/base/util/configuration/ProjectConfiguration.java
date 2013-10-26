/**
 * @author Sam Schreiber
 */

package org.ggp.base.util.configuration;

import java.io.File;

public class ProjectConfiguration {
    /* Game rulesheet repository information */
    private static final String gamesRootDirectoryPath = "games";
    
    public static final File gameImagesDirectory = new File(new File(gamesRootDirectoryPath, "resources"), "images");
}