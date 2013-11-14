package org.ggp.base.apps.server;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;

import org.ggp.base.apps.server.leaderboard.LeaderboardPanel;
import org.ggp.base.apps.server.scheduling.PendingMatch;
import org.ggp.base.apps.server.scheduling.Scheduler;
import org.ggp.base.apps.server.scheduling.SchedulingPanel;
import org.ggp.base.util.crypto.BaseCryptography.EncodedKeyPair;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.presence.PlayerPresence;
import org.ggp.base.util.presence.PlayerPresenceManager.InvalidHostportException;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.ui.GameSelector;
import org.ggp.base.util.ui.JLabelBold;
import org.ggp.base.util.ui.NativeUI;
import org.ggp.base.util.ui.PlayerSelector;

@SuppressWarnings("serial")
public final class Server extends JPanel implements ActionListener
{
	static void createAndShowGUI(Server serverPanel, String title)
	{
		JFrame frame = new JFrame(title);
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

		final Server serverPanel = new Server();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				createAndShowGUI(serverPanel, "Game Server");
			}
		});
	}

	private Game theGame;

	private final JPanel managerPanel;
	private final JTabbedPane matchesTabbedPane;
	private final JPanel gamePanel;
	private final JPanel playersPanel;

	private final SchedulingPanel schedulingPanel;
	private final LeaderboardPanel leaderboardPanel;

	private final List<JComboBox<String>> playerFields;
	private final List<JLabel> roleLabels;
	private final JButton runButton;

	private final JSpinner startClockSpinner;
	private final JSpinner playClockSpinner;
	private final JSpinner repetitionsSpinner;

	private final JCheckBox shouldScramble;
	private final JCheckBox shouldQueue;
	private final JCheckBox shouldDetail;
	private final JCheckBox shouldPublish;
	private final JCheckBox shouldSave;

	private final GameSelector gameSelector;
	private final PlayerSelector playerSelector;
	private final JList<String> playerSelectorList;

	private final Scheduler scheduler;

	public Server()
	{
		super(new GridBagLayout());

		runButton = new JButton(runButtonMethod());
		startClockSpinner = new JSpinner(new SpinnerNumberModel(30,5,600,1));
		playClockSpinner = new JSpinner(new SpinnerNumberModel(15,5,300,1));
		repetitionsSpinner = new JSpinner(new SpinnerNumberModel(1,1,1000,1));
		matchesTabbedPane = new JTabbedPane();

		managerPanel = new JPanel(new GridBagLayout());
		gamePanel = new JPanel(new GridBagLayout());
		playersPanel = new JPanel(new GridBagLayout());

		roleLabels = new ArrayList<JLabel>();
		playerFields = new ArrayList<JComboBox<String>>();
		theGame = null;

		shouldScramble = new JCheckBox("Scramble GDL?", true);
		shouldQueue = new JCheckBox("Queue match?", true);
		shouldDetail = new JCheckBox("Show match details?", true);
		shouldSave = new JCheckBox("Save match to disk?", false);
		shouldPublish = new JCheckBox("Publish match to the web?", false);

		runButton.setEnabled(false);

		gameSelector = new GameSelector();
		playerSelector = new PlayerSelector();
		playerSelectorList = playerSelector.getPlayerSelectorList();

		int nRowCount = 0;
		gamePanel.add(new JLabelBold("Match Setup"), new GridBagConstraints(0, nRowCount++, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 25, 5, 25), 0, 0));
		gamePanel.add(new JLabel("Repository:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 1, 5), 5, 5));
		gamePanel.add(gameSelector.getRepositoryList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 1, 5), 5, 5));
		gamePanel.add(new JLabel("Game:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 5, 5), 5, 5));
		gamePanel.add(gameSelector.getGameList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 5, 5), 5, 5));
		gamePanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 1, 5), 5, 5));
		gamePanel.add(startClockSpinner, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 1, 5), 5, 5));
		gamePanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 5, 5), 5, 5));
		gamePanel.add(playClockSpinner, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 5, 5), 5, 5));
		gamePanel.add(new JLabel("Repetitions:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 5, 5), 5, 5));
		gamePanel.add(repetitionsSpinner, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 5, 5), 5, 5));
		gamePanel.add(shouldScramble, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 5, 0));
		gamePanel.add(shouldQueue, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 5, 0));
		gamePanel.add(shouldDetail, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 5, 0));
		gamePanel.add(shouldSave, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 5, 0));
		gamePanel.add(shouldPublish, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 5, 0));
		gamePanel.add(runButton, new GridBagConstraints(1, nRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		nRowCount = 0;
		playersPanel.add(new JLabelBold("Player List"), new GridBagConstraints(0, nRowCount++, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 25, 5, 25), 0, 0));
		playersPanel.add(new JScrollPane(playerSelectorList), new GridBagConstraints(0, nRowCount++, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 25, 5, 25), 0, 0));
		playersPanel.add(new JButton(addPlayerButtonMethod()), new GridBagConstraints(0, nRowCount, 1, 1, 1.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		playersPanel.add(new JButton(removePlayerButtonMethod()), new GridBagConstraints(1, nRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		playersPanel.add(new JButton(testPlayerButtonMethod()), new GridBagConstraints(2, nRowCount++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		nRowCount = 0;
		managerPanel.add(gamePanel, new GridBagConstraints(0, nRowCount++, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(new JSeparator(), new GridBagConstraints(0, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(playersPanel, new GridBagConstraints(0, nRowCount++, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

		this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(matchesTabbedPane, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

        gameSelector.getGameList().addActionListener(this);
        gameSelector.repopulateGameList();

        schedulingPanel = new SchedulingPanel();
        leaderboardPanel = new LeaderboardPanel();
		matchesTabbedPane.addTab("Overview", new OverviewPanel());

		scheduler = new Scheduler(matchesTabbedPane, schedulingPanel, leaderboardPanel);
		schedulingPanel.setScheduler(scheduler);
		scheduler.start();
	}

	public void setSigningKeys(EncodedKeyPair keys) {
		scheduler.signingKeys = keys;
	}

	class OverviewPanel extends JPanel {
		public OverviewPanel() {
			super(new GridBagLayout());
			add(schedulingPanel, new GridBagConstraints(0, 0, 1, 1, 2.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
			add(leaderboardPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		}
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

            int newRowCount = 11;
            for (int i = 0; i < roles.size(); i++) {
                roleLabels.add(new JLabel(roles.get(i).getName().toString() + ":"));
                playerFields.add(playerSelector.getPlayerSelectorBox());
                playerFields.get(i).setSelectedIndex(i%playerFields.get(i).getModel().getSize());

                gamePanel.add(roleLabels.get(i), new GridBagConstraints(0, newRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 1, 5), 5, 5));
                gamePanel.add(playerFields.get(i), new GridBagConstraints(1, newRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 5, 5));
            }
            gamePanel.add(runButton, new GridBagConstraints(1, newRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

            validate();
            runButton.setEnabled(true);
        }
    }

	private AbstractAction runButtonMethod() {
		return new AbstractAction("Start a new match!") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				int startClock = (Integer)startClockSpinner.getValue();
				int playClock = (Integer)playClockSpinner.getValue();

				List<PlayerPresence> thePlayers = new ArrayList<PlayerPresence>();
				for (JComboBox<String> playerField : playerFields) {
                	String name = playerField.getSelectedItem().toString();
                	thePlayers.add(playerSelector.getPlayerPresence(name));
				}

				synchronized (scheduler) {
					for (int i = 0; i < (Integer)repetitionsSpinner.getValue(); i++) {
						scheduler.addPendingMatch(new PendingMatch("Base", theGame, new ArrayList<PlayerPresence>(thePlayers), -1, startClock, playClock, shouldScramble.isSelected(), shouldQueue.isSelected(), shouldDetail.isSelected(), shouldSave.isSelected(), shouldPublish.isSelected()));
						thePlayers.add(thePlayers.remove(0));  // rotate player roster for repeated matches
						try {
							Thread.sleep(10);
						} catch (InterruptedException ie) {
							;
						}
					}
				}
			}
		};
	}

	private AbstractAction testPlayerButtonMethod() {
		return new AbstractAction("Test") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (playerSelectorList.getSelectedValue() != null) {
					Game testGame = GameRepository.getDefaultRepository().getGame("maze");
					String playerName = playerSelectorList.getSelectedValue().toString();
					List<PlayerPresence> thePlayers = Arrays.asList(new PlayerPresence[]{playerSelector.getPlayerPresence(playerName)});
					scheduler.addPendingMatch(new PendingMatch("Test", testGame, thePlayers, -1, 10, 5, false, false, true, false, false));
				}
			}
		};
	}

	private AbstractAction addPlayerButtonMethod() {
		return new AbstractAction("Add") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				String hostport = JOptionPane.showInputDialog(null, "What is the new player's address?\nPlease use the format \"host:port\".", "Add a player", JOptionPane.QUESTION_MESSAGE, null, null, "127.0.0.1:9147").toString();
				try {
					playerSelector.addPlayer(hostport);
				} catch (InvalidHostportException e) {
					JOptionPane.showMessageDialog(null, "Could not parse the new player's address! Sorry.", "Error adding player", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
	}

	private AbstractAction removePlayerButtonMethod() {
		return new AbstractAction("Remove") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (playerSelectorList.getSelectedValue() != null) {
					playerSelector.removePlayer(playerSelectorList.getSelectedValue().toString());
				}
			}
		};
	}
}