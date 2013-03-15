package org.ggp.base.apps.server;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.ggp.base.apps.server.error.ErrorPanel;
import org.ggp.base.apps.server.history.HistoryPanel;
import org.ggp.base.apps.server.publishing.PublishingPanel;
import org.ggp.base.apps.server.states.StatesPanel;
import org.ggp.base.apps.server.visualization.VisualizationPanel;
import org.ggp.base.server.GameServer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.presence.PlayerPresence;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.ui.GameSelector;
import org.ggp.base.util.ui.NativeUI;
import org.ggp.base.util.ui.PlayerSelector;

@SuppressWarnings("serial")
public final class ServerPanel2 extends JPanel implements ActionListener
{    
	private static void createAndShowGUI(ServerPanel2 serverPanel)
	{
		JFrame frame = new JFrame("Game Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setPreferredSize(new Dimension(1200, 900));
		frame.getContentPane().add(serverPanel);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args)
	{        
	    NativeUI.setNativeUI();
	    GdlPool.caseSensitive = false;
	
		final ServerPanel2 serverPanel = new ServerPanel2();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				createAndShowGUI(serverPanel);
			}
		});
	}
	
	private Game theGame;
	private final JPanel managerPanel;
	private final JTabbedPane matchesTabbedPane;
	private final JTextField playClockTextField;
	
	private final JPanel gamePanel;
	private final JPanel playersPanel;

	private final List<JComboBox> playerFields;
	private final List<JLabel> roleLabels;
	private final JButton runButton;

	private final JTextField startClockTextField;
	private final GameSelector gameSelector;
	private final PlayerSelector playerSelector;
	private final JList playerSelectorList;

	class BoldJLabel extends JLabel {
		public BoldJLabel(String text) {			
			super(text);
			setFont(new Font(getFont().getFamily(), Font.BOLD, getFont().getSize()+2));
		}
	}
	
	public ServerPanel2()
	{
		super(new GridBagLayout());
		
		runButton = new JButton(runButtonMethod());
		startClockTextField = new JTextField("30");
		playClockTextField = new JTextField("15");		
		matchesTabbedPane = new JTabbedPane();
		
		managerPanel = new JPanel(new GridBagLayout());
		gamePanel = new JPanel(new GridBagLayout());
		playersPanel = new JPanel(new GridBagLayout());

		roleLabels = new ArrayList<JLabel>();
		playerFields = new ArrayList<JComboBox>();
		theGame = null;

		runButton.setEnabled(false);
		startClockTextField.setColumns(15);
		playClockTextField.setColumns(15);

		gameSelector = new GameSelector();
		playerSelector = new PlayerSelector();
		playerSelectorList = playerSelector.getPlayerSelectorList();
		
		int nRowCount = 0;
		gamePanel.add(new BoldJLabel("Match Setup"), new GridBagConstraints(0, nRowCount++, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 25, 5, 25), 0, 0));
		gamePanel.add(new JLabel("Repository:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(gameSelector.getRepositoryList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(new JLabel("Game:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(gameSelector.getGameList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(startClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(playClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		gamePanel.add(runButton, new GridBagConstraints(1, nRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		
		nRowCount = 0;
		playersPanel.add(new BoldJLabel("Player List"), new GridBagConstraints(0, nRowCount++, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 25, 5, 25), 0, 0));
		playersPanel.add(new JScrollPane(playerSelectorList), new GridBagConstraints(0, nRowCount++, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 25, 5, 25), 0, 0));
		playersPanel.add(new JButton(addPlayerButtonMethod()), new GridBagConstraints(0, nRowCount, 1, 1, 1.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		playersPanel.add(new JButton(removePlayerButtonMethod()), new GridBagConstraints(1, nRowCount, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		//playersPanel.add(new JButton("Test"), new GridBagConstraints(2, nRowCount++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		nRowCount = 0;
		managerPanel.add(gamePanel, new GridBagConstraints(0, nRowCount++, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JSeparator(), new GridBagConstraints(0, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(playersPanel, new GridBagConstraints(0, nRowCount++, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));

		JPanel matchesPanel = new JPanel(new GridBagLayout());
		matchesPanel.setBorder(new TitledBorder("Matches"));
		matchesPanel.add(matchesTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

		this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(matchesPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		
        gameSelector.getGameList().addActionListener(this);
        gameSelector.repopulateGameList();
	}

	private AbstractAction runButtonMethod() {
		return new AbstractAction("Start a new match!") {
			public void actionPerformed(ActionEvent evt) {
				try {
					String matchId = "BaseServer." + theGame.getKey() + "." + System.currentTimeMillis();
					
					int startClock = Integer.valueOf(startClockTextField.getText());
					int playClock = Integer.valueOf(playClockTextField.getText());
					Match match = new Match(matchId, -1, startClock, playClock, theGame);

					List<String> hosts = new ArrayList<String>(playerFields.size());
					List<Integer> ports = new ArrayList<Integer>(playerFields.size());
					List<String> playerNames = new ArrayList<String>(playerFields.size());
					for (JComboBox playerField : playerFields) {
	                    try {
	                    	String name = playerField.getSelectedItem().toString();
	                    	PlayerPresence player = playerSelector.getPlayerPresence(name);
	                        hosts.add(player.getHost());
	                        ports.add(player.getPort());
	                        playerNames.add(name);
	                    } catch(Exception ex) {
	                        ex.printStackTrace();
	                        return;
	                    } 					    
					}

					HistoryPanel historyPanel = new HistoryPanel();
					ErrorPanel errorPanel = new ErrorPanel();
					VisualizationPanel visualizationPanel = new VisualizationPanel(theGame);
					StatesPanel statesPanel = new StatesPanel();

					JTabbedPane tab = new JTabbedPane();
					tab.addTab("History", historyPanel);
					tab.addTab("Error", errorPanel);
					tab.addTab("Visualization", visualizationPanel);
					tab.addTab("States", statesPanel);
					matchesTabbedPane.addTab(matchId, tab);
					matchesTabbedPane.setSelectedIndex(matchesTabbedPane.getTabCount()-1);
					
					GameServer gameServer = new GameServer(match, hosts, ports, playerNames);
					gameServer.addObserver(errorPanel);
					gameServer.addObserver(historyPanel);
					gameServer.addObserver(visualizationPanel);					
					gameServer.addObserver(statesPanel);
					gameServer.start();
					
					// TODO: Implement this as a checkbox upon match creation
					tab.addTab("Publishing", new PublishingPanel(gameServer));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	private AbstractAction addPlayerButtonMethod() {
		return new AbstractAction("Add") {
			public void actionPerformed(ActionEvent evt) {
				String hostport = JOptionPane.showInputDialog(null, "Please enter the host:port of the player to add.", "FOO", JOptionPane.QUESTION_MESSAGE, null, null, "127.0.0.1:9147").toString();
				playerSelector.addPlayer(hostport);
			}
		};
	}
	
	private AbstractAction removePlayerButtonMethod() {
		return new AbstractAction("Remove") {
			public void actionPerformed(ActionEvent evt) {
				playerSelector.removePlayer(playerSelectorList.getSelectedValue().toString());
			}
		};
	}	

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == gameSelector.getGameList()) {
            theGame = gameSelector.getSelectedGame();

            for (int i = 0; i < roleLabels.size(); i++) {
                gamePanel.remove(roleLabels.get(i));
                gamePanel.remove(playerFields.get(i));
            }

            roleLabels.clear();
            playerFields.clear();

            validate();
            runButton.setEnabled(false);
            if (theGame == null)
                return;            

            StateMachine stateMachine = new ProverStateMachine();
            stateMachine.initialize(theGame.getRules());
            List<Role> roles = stateMachine.getRoles();
            
            int newRowCount = 7;
            for (int i = 0; i < roles.size(); i++) {
                roleLabels.add(new JLabel(roles.get(i).getName().toString() + ":"));
                playerFields.add(playerSelector.getPlayerSelectorBox());
                playerFields.get(i).setSelectedIndex(i%playerFields.get(i).getModel().getSize());

                gamePanel.add(roleLabels.get(i), new GridBagConstraints(0, newRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
                gamePanel.add(playerFields.get(i), new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
            }
            gamePanel.add(runButton, new GridBagConstraints(1, newRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

            validate();
            runButton.setEnabled(true);
        }        
    }
}
