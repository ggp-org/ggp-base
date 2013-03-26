package org.ggp.base.apps.tourney;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.reflection.ProjectSearcher;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.ui.GameSelector;
import org.ggp.base.util.ui.NativeUI;



/**
 * Tourney is a locally-run application which allows you to run a large number
 * of matches of a single game between multiple players on the local machine.
 * This can be used to understand which players are stronger than which other
 * players on a particular game, and is a great tool for performing automated
 * experiments on players you're developing.
 * 
 * Since both players will be running on the same machine, they may run into
 * resource contention issues: for example, both may attempt to use all of the
 * available memory and processor cycles. Tourney does not prevent this: for
 * best performance, ensure that at least one of the two players is a simple
 * player (like RandomGamer, LegalGamer, or SimpleSearchLightGamer) that will
 * not attempt to use the majority of the machine's resources. Ensuring that
 * resource contention between multiple resource-intensive players is resolved
 * fairly is well beyond the scope of Tourney.
 * 
 * To get around this problem, there's a continuously-running online tournament
 * called Tiltyard running at http://tiltyard.ggp.org/ which can be used to test
 * your player against other real players on a wide variety of games. The online
 * Tiltyard also aggregates statistics and player rankings based on the matches
 * that are played on it.
 * 
 * NOTE: Long term, this will be phased out in favor of more advanced features in
 * the regular ServerPanel app for managing tournaments and repeatedly running
 * matches and aggregating statistics about their outcomes.
 * 
 * @author Sam Schreiber
 */
@SuppressWarnings("serial")
public final class Tourney extends JPanel implements ActionListener {
    private static void createAndShowGUI(Tourney playerPanel) {
        JFrame frame = new JFrame("GGP Tourney");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setPreferredSize(new Dimension(1024, 768));
        frame.getContentPane().add(playerPanel);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        NativeUI.setNativeUI();
        
        final Tourney tourneyPanel = new Tourney();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(tourneyPanel);
            }
        });
    }

    private final JButton runButton;
    private final JTextField playClockTextField;
    private final JTextField startClockTextField;
    private final JTextField numRepsTextField;  
    
    private List<Class<?>> gamers = ProjectSearcher.getAllClassesThatAre(Gamer.class);
    private List<JComboBox> playerBoxes;
    private final JPanel playerBoxesPanel;
    
    private final TourneyEventsPanel eventsPanel;

    private final GameSelector gameSelector;    
    
    private JComboBox getFreshPlayerComboBox() {
        JComboBox newBox = new JComboBox();

        List<Class<?>> gamersCopy = new ArrayList<Class<?>>(gamers);
        for (Class<?> gamer : gamersCopy) {
            Gamer g;
            try {
                g = (Gamer) gamer.newInstance();
                if (!g.isComputerPlayer()) {
                	throw new Exception("Tourney only considers computer players");
                }
                newBox.addItem(g.getName());
            } catch (Exception ex) {
            	gamers.remove(gamer);
            }            
        }	

        newBox.setSelectedItem("Random");
        return newBox;
    }
    
    public Tourney() {
        super(new GridBagLayout());

        // Create the game-selection controls
        startClockTextField = new JTextField("30");
        playClockTextField = new JTextField("15");
        numRepsTextField = new JTextField("100");

        // Create the player-selection controls
        playerBoxes = new ArrayList<JComboBox>();
        playerBoxesPanel = new JPanel(new GridBagLayout());        
        
        // Create the panel at the bottom with the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        runButton = new JButton(runButtonMethod());
        runButton.setEnabled(false);
        buttonPanel.add(runButton);
        
        // Create the panel that shows the actual events
        eventsPanel = new TourneyEventsPanel();                

        gameSelector = new GameSelector();        
        
        int nGridRow = 0;
        JPanel managerPanel = new JPanel(new GridBagLayout());
        managerPanel.setBorder(new TitledBorder("Manager"));        
        managerPanel.add(new JLabel("Repository:"), new GridBagConstraints(0, nGridRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(gameSelector.getRepositoryList(), new GridBagConstraints(1, nGridRow++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Game:"), new GridBagConstraints(0, nGridRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(gameSelector.getGameList(), new GridBagConstraints(1, nGridRow++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JSeparator(), new GridBagConstraints(0, nGridRow++, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, nGridRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(startClockTextField, new GridBagConstraints(1, nGridRow++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, nGridRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playClockTextField, new GridBagConstraints(1, nGridRow++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Repetitions:"), new GridBagConstraints(0, nGridRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(numRepsTextField, new GridBagConstraints(1, nGridRow++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JSeparator(), new GridBagConstraints(0, nGridRow++, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playerBoxesPanel, new GridBagConstraints(0, nGridRow++, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));        
        managerPanel.add(buttonPanel, new GridBagConstraints(1, nGridRow++, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        JPanel gamesPanel = new JPanel(new GridBagLayout());
        gamesPanel.setBorder(new TitledBorder("Tourney Games"));
        gamesPanel.add(eventsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        this.add(managerPanel, new GridBagConstraints(0, 0, 1, 2, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        this.add(gamesPanel, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        
        validate();
        
        gameSelector.getGameList().addActionListener(this);
        gameSelector.repopulateGameList();
    }

    private void runTourney() {
        try {
            List<Class<?>> thePlayers = new ArrayList<Class<?>>();
            for(int i = 0; i < playerBoxes.size(); i++) {
                thePlayers.add(gamers.get(playerBoxes.get(i).getSelectedIndex()));
            }

            int playClock = Integer.parseInt(playClockTextField.getText());
            int startClock = Integer.parseInt(startClockTextField.getText());
            int numReps = Integer.parseInt(numRepsTextField.getText());
            
            Match theMatchModel = new Match("MatchID", -1, startClock, playClock, theGame);
            
            TourneyManager theManager = new TourneyManager(thePlayers, theMatchModel, gameName, numReps, eventsPanel);
            theManager.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AbstractAction runButtonMethod() {
        return new AbstractAction("Run Tourney") {
            public void actionPerformed(ActionEvent evt) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        runTourney();
                    }
                });
            }
        };
    }
    
    private String gameName;
    private Game theGame;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == gameSelector.getGameList()) {
            theGame = gameSelector.getSelectedGame();
            if (theGame == null) {
                runButton.setEnabled(false);
                return;
            }
            
            StateMachine stateMachine = new ProverStateMachine();
            stateMachine.initialize(theGame.getRules());
            List<Role> roles = stateMachine.getRoles();
            int nRoles = roles.size();

            while(playerBoxes.size() > nRoles) {
                playerBoxes.remove(playerBoxes.size()-1);       
            }

            while(playerBoxes.size() < nRoles) {
                playerBoxes.add(getFreshPlayerComboBox());
            }

            List<Integer> currentSelections = new ArrayList<Integer>();
            for(int i = 0; i < playerBoxes.size(); i++) {
                currentSelections.add(playerBoxes.get(i).getSelectedIndex());
            }

            playerBoxesPanel.removeAll();
            for(int i = 0; i < roles.size(); i++) {
                playerBoxesPanel.add(new JLabel("Player " + (i+1) + " Type:"), new GridBagConstraints(0, i, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
                playerBoxesPanel.add(playerBoxes.get(i), new GridBagConstraints(1, i, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
            }

            for(int i = 0; i < playerBoxes.size(); i++) {
                playerBoxes.get(i).setSelectedIndex(currentSelections.get(i));
            }

            playerBoxesPanel.validate();  
            validate();

            runButton.setEnabled(true);  
        }
    }    
}