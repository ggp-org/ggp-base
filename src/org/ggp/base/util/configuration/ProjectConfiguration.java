/**
 * ProjectConfiguration handles the project-specific directory settings.
 * This class stores the paths of the game directory so it can quickly be changed and overridden.
 *
 * @author Sam Schreiber
 */

package org.ggp.base.util.configuration;

import java.io.File;

public class ProjectConfiguration {
    /* Game rulesheet repository information */
    private static final String gamesRootDirectoryPath = "games";

    public static final File gameImagesDirectory = new File(new File(gamesRootDirectoryPath, "resources"), "images");
}