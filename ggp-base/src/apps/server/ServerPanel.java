package apps.server;

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

import server.GameServer;
import util.game.Game;
import util.match.Match;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.common.GameSelector;
import apps.common.NativeUI;
import apps.server.error.ErrorPanel;
import apps.server.history.HistoryPanel;
import apps.server.publishing.PublishingPanel;
import apps.server.visualization.VisualizationPanel;

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
	
		final ServerPanel serverPanel = new ServerPanel();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				createAndShowGUI(serverPanel);
			}
		});
	}
	
    private Integer defaultPort = 9147;	

	private Game theGame;
	private final List<JTextField> hostTextFields;
	private final JPanel managerPanel;
	private final JTabbedPane matchesTabbedPane;
	private final JTextField playClockTextField;

	private final List<JTextField> portTextFields;
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
		hostTextFields = new ArrayList<JTextField>();
		portTextFields = new ArrayList<JTextField>();
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
					String matchId = "Match." + System.currentTimeMillis();
					
					int startClock = Integer.valueOf(serverPanel.startClockTextField.getText());
					int playClock = Integer.valueOf(serverPanel.playClockTextField.getText());
					Match match = new Match(matchId, startClock, playClock, serverPanel.theGame);

					List<String> hosts = new ArrayList<String>(serverPanel.hostTextFields.size());
					for (JTextField textField : serverPanel.hostTextFields)
					{
						hosts.add(textField.getText());
					}
					List<Integer> ports = new ArrayList<Integer>(serverPanel.portTextFields.size());
					for (JTextField textField : serverPanel.portTextFields)
					{
						ports.add(Integer.valueOf(textField.getText()));
					}
					List<String> playerNames = new ArrayList<String>(serverPanel.playerNameTextFields.size());
					for (JTextField textField : serverPanel.playerNameTextFields)
					{
						playerNames.add(textField.getText());
					}

					HistoryPanel historyPanel = new HistoryPanel();
					ErrorPanel errorPanel = new ErrorPanel();
					VisualizationPanel visualizationPanel = new VisualizationPanel(theGame);

					JTabbedPane tab = new JTabbedPane();
					tab.addTab("History", historyPanel);
					tab.addTab("Error", errorPanel);
					tab.addTab("Visualization", visualizationPanel);
					serverPanel.matchesTabbedPane.addTab(matchId, tab);
					serverPanel.matchesTabbedPane.setSelectedIndex(serverPanel.matchesTabbedPane.getTabCount()-1);
					
					GameServer gameServer = new GameServer(match, hosts, ports, playerNames);
					gameServer.addObserver(errorPanel);
					gameServer.addObserver(historyPanel);
					gameServer.addObserver(visualizationPanel);					
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
                managerPanel.remove(hostTextFields.get(i));
                managerPanel.remove(portTextFields.get(i));
                managerPanel.remove(playerNameTextFields.get(i));
            }

            roleLabels.clear();
            hostTextFields.clear();
            portTextFields.clear();
            playerNameTextFields.clear();

            validate();
            runButton.setEnabled(false);
            if (theGame == null)
                return;            

            StateMachine stateMachine = new ProverStateMachine();
            stateMachine.initialize(theGame.getRules());
            List<Role> roles = stateMachine.getRoles();
            Integer tempDefaultPort = defaultPort;
            
            int newRowCount = 7;
            for (int i = 0; i < roles.size(); i++) {
                roleLabels.add(new JLabel(roles.get(i).getName().toString() + ":"));
                hostTextFields.add(new JTextField("localhost"));
                portTextFields.add(new JTextField(tempDefaultPort.toString()));
                playerNameTextFields.add(new JTextField("defaultPlayerName"));
                tempDefaultPort++;

                hostTextFields.get(i).setColumns(15);
                portTextFields.get(i).setColumns(15);
                playerNameTextFields.get(i).setColumns(15);

                managerPanel.add(roleLabels.get(i), new GridBagConstraints(0, newRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
                managerPanel.add(hostTextFields.get(i), new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
                managerPanel.add(portTextFields.get(i), new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
                managerPanel.add(playerNameTextFields.get(i),  new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
            }
            managerPanel.add(runButton, new GridBagConstraints(1, newRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

            validate();
            runButton.setEnabled(true);
        }        
    }
}
