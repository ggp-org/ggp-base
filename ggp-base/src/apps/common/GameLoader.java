package apps.common;

import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import util.configuration.ProjectConfiguration;
import util.configuration.ResourceLoader;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;

public class GameLoader {
    public static List<Gdl> loadGame(String gameName) {
        // TODO: Eventually this method should become deprecated,
        // in favor of the ResourceLoader version. Possibly, this
        // entire class should be deprecated.
        return ResourceLoader.loadGame(gameName);
    }
    
    public static List<Gdl> loadGameUsingPrompt() {
        JFileChooser fileChooser = new JFileChooser(ProjectConfiguration.gameRulesheetsPath);
        int rval = fileChooser.showOpenDialog(new JPanel());
        if (rval != JFileChooser.APPROVE_OPTION)
            return null;

        List<Gdl> description = null;
        try {
            File file = fileChooser.getSelectedFile();
            description = KifReader.read(file.getAbsolutePath());
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
        return description;
    }
}