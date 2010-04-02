package apps.server;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import server.GameServer;
import util.configuration.ProjectConfiguration;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import util.match.Match;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import util.symbol.factory.exceptions.SymbolFormatException;
import apps.common.NativeUI;
import apps.server.error.ErrorPanel;
import apps.server.history.HistoryPanel;
import apps.server.visualization.VisualizationPanel;

@SuppressWarnings("serial")
public final class ServerPanel extends JPanel
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

	private List<Gdl> description;
	private final List<JTextField> hostTextFields;
	private final JPanel managerPanel;
	private final JTabbedPane matchesTabbedPane;
	private final JTextField matchIdTextField;
	private final JTextField playClockTextField;

	private final List<JTextField> portTextFields;
	private final List<JTextField> playerNameTextFields;
	private final List<JLabel> roleLabels;
	private final JButton runButton;
	private final JButton sourceButton;

	private final JTextField sourceTextField;

	private final JTextField startClockTextField;

	public ServerPanel()
	{
		super(new GridBagLayout());
		
		runButton = new JButton(runButtonMethod(this));
		sourceButton = new JButton(sourceButtonMethod(this));
		sourceTextField = new JTextField("Click to select a .kif file");
		matchIdTextField = new JTextField("Match.default");
		startClockTextField = new JTextField("10");
		playClockTextField = new JTextField("30");
		managerPanel = new JPanel(new GridBagLayout());
		matchesTabbedPane = new JTabbedPane();

		roleLabels = new ArrayList<JLabel>();
		hostTextFields = new ArrayList<JTextField>();
		portTextFields = new ArrayList<JTextField>();
		playerNameTextFields = new ArrayList<JTextField>();
		description = null;

		runButton.setEnabled(false);
		sourceTextField.setEnabled(false);
		sourceTextField.setColumns(15);
		startClockTextField.setColumns(15);
		playClockTextField.setColumns(15);

		managerPanel.setBorder(new TitledBorder("Manager"));
		managerPanel.add(sourceButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		managerPanel.add(sourceTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Match Id:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(matchIdTextField, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(startClockTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(playClockTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(runButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel matchesPanel = new JPanel(new GridBagLayout());
		matchesPanel.setBorder(new TitledBorder("Matches"));
		matchesPanel.add(matchesTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

		this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(matchesPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));		
	}

	private AbstractAction runButtonMethod(final ServerPanel serverPanel)
	{
		return new AbstractAction("Run")
		{
			public void actionPerformed(ActionEvent evt)
			{
				try
				{
					List<Gdl> description = serverPanel.description;
					String matchId = serverPanel.matchIdTextField.getText();
					if(matchId.equals("Match.default"))
					    matchId = "Match." + System.currentTimeMillis();
					
					int startClock = Integer.valueOf(serverPanel.startClockTextField.getText());
					int playClock = Integer.valueOf(serverPanel.playClockTextField.getText());
					Match match = new Match(matchId, startClock, playClock, description);

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
					VisualizationPanel visualizationPanel = new VisualizationPanel(gameName);

					JTabbedPane tab = new JTabbedPane();
					tab.addTab("History", historyPanel);
					tab.addTab("Error", errorPanel);
					tab.addTab("Visualization", visualizationPanel);
					serverPanel.matchesTabbedPane.addTab(matchId, tab);
					serverPanel.matchesTabbedPane.setSelectedIndex(serverPanel.matchesTabbedPane.getTabCount()-1);

					Integer tempDefaultPort = defaultPort;
					for(JTextField tf : portTextFields)
					{
						tf.setText(tempDefaultPort.toString());
						tempDefaultPort++;
					}
					
					GameServer gameServer = new GameServer(match, hosts, ports, playerNames);
					gameServer.addObserver(errorPanel);
					gameServer.addObserver(historyPanel);
					gameServer.addObserver(visualizationPanel);
					gameServer.start();					
				}
				catch (Exception e)
				{
					// Do nothing.
				}
			}
		};
	}

	private Integer defaultPort = 9147;
	private String gameName = "";
	private AbstractAction sourceButtonMethod(final ServerPanel serverPanel)
	{
		return new AbstractAction("Source")
		{
			public void actionPerformed(ActionEvent evt)
			{
				JFileChooser fileChooser = new JFileChooser(ProjectConfiguration.gameRulesheetsPath);
				if (fileChooser.showOpenDialog(ServerPanel.this) == JFileChooser.APPROVE_OPTION)
				{					
					try {
						File file = fileChooser.getSelectedFile();
						description = KifReader.read(file.getAbsolutePath());
						sourceTextField.setText(file.getName());
						gameName = file.getName().replaceAll("\\..*?$",""); //strip file ending

						for (int i = 0; i < roleLabels.size(); i++)
						{
							serverPanel.managerPanel.remove(roleLabels.get(i));
							serverPanel.managerPanel.remove(hostTextFields.get(i));
							serverPanel.managerPanel.remove(portTextFields.get(i));
							serverPanel.managerPanel.remove(playerNameTextFields.get(i));
						}

						roleLabels.clear();
						hostTextFields.clear();
						portTextFields.clear();
						playerNameTextFields.clear();

						serverPanel.validate();

						StateMachine stateMachine = new ProverStateMachine();
						stateMachine.initialize(description);
						List<Role> roles = stateMachine.getRoles();
						Integer tempDefaultPort = defaultPort;
						
						for (int i = 0; i < roles.size(); i++)
						{
							roleLabels.add(new JLabel(roles.get(i).getName().toString() + ":"));
							hostTextFields.add(new JTextField("localhost"));
							portTextFields.add(new JTextField(tempDefaultPort.toString()));
							playerNameTextFields.add(new JTextField("defaultPlayerName"));
							tempDefaultPort++;

							hostTextFields.get(i).setColumns(15);
							portTextFields.get(i).setColumns(15);
							playerNameTextFields.get(i).setColumns(15);

							serverPanel.managerPanel.add(roleLabels.get(i), new GridBagConstraints(0, 4 + 3 * i, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
							serverPanel.managerPanel.add(hostTextFields.get(i), new GridBagConstraints(1, 4 + 3 * i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
							serverPanel.managerPanel.add(portTextFields.get(i), new GridBagConstraints(1, 5 + 3 * i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
							serverPanel.managerPanel.add(playerNameTextFields.get(i),  new GridBagConstraints(1, 6 + 3 * i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
						}
						serverPanel.managerPanel.add(runButton, new GridBagConstraints(1, 4 + 3 * roles.size(), 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

						serverPanel.validate();

						runButton.setEnabled(true);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (SymbolFormatException e) {
						e.printStackTrace();
					} catch (GdlFormatException e) {
						e.printStackTrace();
					}					
				}
			}
		};
	}
}
