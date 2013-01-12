package org.ggp.base.apps.server;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.ui.GameSelector;
import org.ggp.base.util.ui.NativeUI;


@SuppressWarnings("serial")
public final class ServerPanel extends JPanel implements ActionListener
{    
	private static void createAndShowGUI(ServerPanel serverPanel)
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
	
		final ServerPanel serverPanel = new ServerPanel();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				createAndShowGUI(serverPanel);
			}
		});
	}
	
	private Game theGame;
	private final List<JTextField> hostportTextFields;
	private final JPanel managerPanel;
	private final JTabbedPane matchesTabbedPane;
	private final JTextField playClockTextField;

	private final List<JTextField> playerNameTextFields;
	private final List<JLabel> roleLabels;
	private final JButton runButton;

	private final JTextField startClockTextField;
	private final GameSelector gameSelector;

	public ServerPanel()
	{
		super(new GridBagLayout());
		
		runButton = new JButton(runButtonMethod(this));
		startClockTextField = new JTextField("30");
		playClockTextField = new JTextField("15");
		managerPanel = new JPanel(new GridBagLayout());
		matchesTabbedPane = new JTabbedPane();

		roleLabels = new ArrayList<JLabel>();
		hostportTextFields = new ArrayList<JTextField>();
		playerNameTextFields = new ArrayList<JTextField>();
		theGame = null;

		runButton.setEnabled(false);
		startClockTextField.setColumns(15);
		playClockTextField.setColumns(15);

		gameSelector = new GameSelector();
		
		int nRowCount = 0;
		managerPanel.setBorder(new TitledBorder("Manager"));
		managerPanel.add(new JLabel("Repository:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(gameSelector.getRepositoryList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Game:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(gameSelector.getGameList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JSeparator(), new GridBagConstraints(0, nRowCount++, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(startClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(playClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JSeparator(), new GridBagConstraints(0, nRowCount++, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(runButton, new GridBagConstraints(1, nRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel matchesPanel = new JPanel(new GridBagLayout());
		matchesPanel.setBorder(new TitledBorder("Matches"));
		matchesPanel.add(matchesTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

		this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(matchesPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		
        gameSelector.getGameList().addActionListener(this);
        gameSelector.repopulateGameList();		
	}

	private AbstractAction runButtonMethod(final ServerPanel serverPanel)
	{
		return new AbstractAction("Run")
		{
			public void actionPerformed(ActionEvent evt)
			{
				try
				{
					String matchId = "BaseServer." + serverPanel.theGame.getKey() + "." + System.currentTimeMillis();
					
					int startClock = Integer.valueOf(serverPanel.startClockTextField.getText());
					int playClock = Integer.valueOf(serverPanel.playClockTextField.getText());
					Match match = new Match(matchId, startClock, playClock, serverPanel.theGame);

					List<String> hosts = new ArrayList<String>(serverPanel.hostportTextFields.size());
					List<Integer> ports = new ArrayList<Integer>(serverPanel.hostportTextFields.size());
					for (JTextField textField : serverPanel.hostportTextFields)
					{
	                    try {
	                        String[] splitAddress = textField.getText().split(":");
	                        String hostname = splitAddress[0];
	                        int port = Integer.parseInt(splitAddress[1]);
	                        
	                        hosts.add(hostname);
	                        ports.add(port);                    
	                    } catch(Exception ex) {
	                        ex.printStackTrace();
	                        return;
	                    } 					    
					}
					List<String> playerNames = new ArrayList<String>(serverPanel.playerNameTextFields.size());
					for (JTextField textField : serverPanel.playerNameTextFields)
					{
						playerNames.add(textField.getText());
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
					serverPanel.matchesTabbedPane.addTab(matchId, tab);
					serverPanel.matchesTabbedPane.setSelectedIndex(serverPanel.matchesTabbedPane.getTabCount()-1);
					
					GameServer gameServer = new GameServer(match, hosts, ports, playerNames);
					gameServer.addObserver(errorPanel);
					gameServer.addObserver(historyPanel);
					gameServer.addObserver(visualizationPanel);					
					gameServer.addObserver(statesPanel);
					gameServer.start();
					
					tab.addTab("Publishing", new PublishingPanel(gameServer));
				}
				catch (Exception e)
				{
					// Do nothing.
				}
			}
		};
	}

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == gameSelector.getGameList()) {
            theGame = gameSelector.getSelectedGame();

            for (int i = 0; i < roleLabels.size(); i++)
            {
                managerPanel.remove(roleLabels.get(i));
                managerPanel.remove(hostportTextFields.get(i));
                managerPanel.remove(playerNameTextFields.get(i));
            }

            roleLabels.clear();
            hostportTextFields.clear();
            playerNameTextFields.clear();

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
                hostportTextFields.add(new JTextField("" + i + ".player.ggp.org:80"));
                playerNameTextFields.add(new JTextField("defaultPlayerName"));

                hostportTextFields.get(i).setColumns(15);
                playerNameTextFields.get(i).setColumns(15);

                managerPanel.add(roleLabels.get(i), new GridBagConstraints(0, newRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
                managerPanel.add(hostportTextFields.get(i), new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
                managerPanel.add(playerNameTextFields.get(i),  new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
            }
            managerPanel.add(runButton, new GridBagConstraints(1, newRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

            validate();
            runButton.setEnabled(true);
        }        
    }
}
