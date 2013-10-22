package org.ggp.base.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

import org.ggp.base.util.files.FileUtils;
import org.junit.Assert;
import org.junit.Test;


public class NoTabsInRulesheetsTest extends Assert {
    // Check that GGP-Base's games use spaces, not tabs.
    @Test
    public void testNoTabsInRulesheets() {
        File testGamesFolder = new File("games", "test");
        assertTrue(testGamesFolder.isDirectory());
        
        for (File gameFile : testGamesFolder.listFiles(new KifFileFilter())) {
            String fileContents = FileUtils.readFileAsString(gameFile);
            assertFalse("The game "+gameFile+" contains tabs. Run the main method in NoTabsInRulesheetsTest to fix this.", fileContents.contains("\t"));
        }
    }
    
    // Modify the test games to use spaces instead of tabs.
    public static void main(String[] args) throws Exception {
        File testGamesFolder = new File("games", "test");
        assertTrue(testGamesFolder.isDirectory());
        
        for (File gameFile : testGamesFolder.listFiles(new KifFileFilter())) {
            String fileContents = FileUtils.readFileAsString(gameFile);
            String newContents = fileContents.replaceAll("\t", "    "); //four spaces
            overwriteFileWithString(gameFile, newContents);
        }
    }
    
    static void overwriteFileWithString(File file, String newContents) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.append(newContents);
        writer.close();
    }
	
	static class KifFileFilter implements FileFilter {
	    @Override
	    public boolean accept(File pathname) {
	        return pathname.getName().endsWith(".kif");
	    }
	}    
}
