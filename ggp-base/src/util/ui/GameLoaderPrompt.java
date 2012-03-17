package util.ui;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import util.configuration.ProjectConfiguration;
import util.game.Game;
import util.game.GameRepository;

public class GameLoaderPrompt {
    public static Game loadGameUsingPrompt() {
        NativeUI.setNativeUI();
        JFileChooser fileChooser = new JFileChooser(ProjectConfiguration.gameRulesheetsPath);
        int rval = fileChooser.showOpenDialog(new JPanel());
        if (rval != JFileChooser.APPROVE_OPTION)
            return null;

        // TODO: What if, in the future, the default repository is not the local repository?
        // What we'd actually like is to display a list of games in the default repository,
        // not a list of games pulled from the local hard drive.
        return GameRepository.getDefaultRepository().getGame(fileChooser.getSelectedFile().getName().replace(".kif", ""));        
    }
}