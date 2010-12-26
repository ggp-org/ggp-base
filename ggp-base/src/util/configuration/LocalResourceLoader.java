package util.configuration;

import java.awt.Image;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import util.files.FileUtils;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import util.logging.GamerLogger;

/**
 * LocalResourceLoader loads locally-stored project resources.
 * 
 * It works regardless of whether it is being called in a stand-alone app,
 * a self-executing JAR, or a web-based applet. For this reason, it is the
 * preferred tool for accessing project resources.
 * 
 * @author Sam
 */
public class LocalResourceLoader {
    private static Object loadResourcesFrom = null;
    
    public static void setLocalResourceLoader(Object o) {
        loadResourcesFrom = o;
    }
    
    public static List<Gdl> loadGame(String gameName) {
        try {            
            String gameDirectory = ProjectConfiguration.gameRulesheetsPath;
            return KifReader.read(gameDirectory + gameName + ".kif");
        } catch(Exception e) {
            try {
                String resourceName = File.separator + ProjectConfiguration.gameRulesheetsDirectory + File.separator + gameName + ".kif";
                resourceName = resourceName.replace('\\', '/');
                InputStream ruleStream = loadResourcesFrom.getClass().getResourceAsStream(resourceName);
                if(ruleStream == null) System.err.println("ruleStream is NULL for: " + resourceName);
                return KifReader.readStream(ruleStream);
            } catch(Exception ee) {
                GamerLogger.logStackTrace("Resources", e);
                GamerLogger.logStackTrace("Resources", ee);
                return null;
            }
        }
    }
    
    public static String loadStylesheet(String xslName) {
        try {
            File xslFile = new File(ProjectConfiguration.gameStylesheetsDirectory, xslName + ".xsl");
            return FileUtils.readFileAsString(xslFile);
        } catch(Exception e) {
            try {
                String resourceName = File.separator + ProjectConfiguration.gameStylesheetsDirectory + File.separator + xslName + ".xsl";
                resourceName = resourceName.replace('\\', '/');
                InputStream styleStream = loadResourcesFrom.getClass().getResourceAsStream(resourceName);
                if(styleStream == null) System.err.println("ruleStream is NULL for: " + resourceName);
                return FileUtils.readFileAsString(styleStream);
            } catch(Exception ee) {
                GamerLogger.logStackTrace("Resources", e);
                GamerLogger.logStackTrace("Resources", ee);
                return null;
            }
        }        
    }    
    
    public static Image loadImage(String imageName) {
        return loadImage("", imageName);
    }    
    
    public static Image loadImage(String dirName, String imageName) {
        try {
            File file = new File(ProjectConfiguration.gameImagesPath + dirName, imageName);            
            return ImageIO.read(file);
        } catch(Exception e) {
            try {
                File file = new File(File.separator + ProjectConfiguration.gameImagesDirectory + File.separator + dirName, imageName);            
                String resourceName = file.getPath();
                resourceName = resourceName.replace('\\', '/');
                URL imageLocation = loadResourcesFrom.getClass().getResource(resourceName);
                if(imageLocation == null)
                    System.err.println("Could not open: " + resourceName + ", based on loading from: " + loadResourcesFrom.getClass().getSimpleName());
                ImageIcon icon = new ImageIcon(imageLocation);
                return icon.getImage();            
            } catch(Exception ee) {
                GamerLogger.logStackTrace("Resources", e);
                GamerLogger.logStackTrace("Resources", ee);
                return null;
            }
        }
    }
}