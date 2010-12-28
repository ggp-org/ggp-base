package apps.common;

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

        return GameRepository.getDefaultRepository().getGame(fileChooser.getSelectedFile().getName().replace(".kif", ""));        
    }
}