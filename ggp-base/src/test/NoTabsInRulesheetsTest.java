package test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import util.files.FileUtils;
import util.files.KifFileFilter;

public class NoTabsInRulesheetsTest extends Assert {
    //Check that GGP-Base's games use spaces, not tabs.
    @Test
    public void testNoTabsInRulesheets() {
        File testGamesFolder = new File("games", "test");
        assertTrue(testGamesFolder.isDirectory());
        
        for (File gameFile : testGamesFolder.listFiles(new KifFileFilter())) {
            String fileContents = FileUtils.readFileAsString(gameFile);
            assertFalse("The game "+gameFile+" contains tabs. Run the main method in NoTabsInRulesheetsTest to fix this.", fileContents.contains("\t"));
        }
    }
    
    //Modify the test games to use spaces instead of tabs.
    public static void main(String[] args) throws Exception {
        File testGamesFolder = new File("games", "test");
        assertTrue(testGamesFolder.isDirectory());
        
        for (File gameFile : testGamesFolder.listFiles(new KifFileFilter())) {
            String fileContents = FileUtils.readFileAsString(gameFile);
            String newContents = fileContents.replaceAll("\t", "    "); //four spaces
            FileUtils.overwriteFileWithString(gameFile, newContents);
        }
    }
}
