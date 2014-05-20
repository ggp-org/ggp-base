package org.ggp.base.apps.player;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.ggp.base.apps.player.config.ConfigPanel;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.match.MatchPanel;
import org.ggp.base.apps.player.network.NetworkPanel;
import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.reflection.ProjectSearcher;
import org.ggp.base.util.ui.NativeUI;

import com.google.common.collect.Lists;


@SuppressWarnings("serial")
public final class Player extends JPanel
{
	private static void createAndShowGUI(Player playerPanel)
	{
		JFrame frame = new JFrame("Game Player");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setPreferredSize(new Dimension(1024, 768));
		frame.getContentPane().add(playerPanel);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException
	{
	    NativeUI.setNativeUI();

	    final Player playerPanel = new Player();
	    javax.swing.SwingUtilities.invokeLater(new Runnable()
	    {

		@Override
		public void run()
		{
		    createAndShowGUI(playerPanel);
		}
	    });
	}

	private final JButton createButton;
	private final JTabbedPane playersTabbedPane;

	private final JTextField portTextField;

	private final JComboBox<String> typeComboBox;

	private Integer defaultPort = 9147;

	private List<Class<? extends Gamer>> gamers = Lists.newArrayList(ProjectSearcher.GAMERS.getConcreteClasses());

	public Player()
	{
		super(new GridBagLayout());

		portTextField = new JTextField(defaultPort.toString());
		typeComboBox = new JComboBox<String>();
		createButton = new JButton(createButtonMethod());
		playersTabbedPane = new JTabbedPane();

		portTextField.setColumns(15);

		// Sort the list of gamers before displaying it to the user
		java.util.Collections.sort(gamers, new Comparator<Class<? extends Gamer>>() {
			@Override
			public int compare(Class<? extends Gamer> left, Class<? extends Gamer> right) {
				return left.getSimpleName().compareTo(right.getSimpleName());
			}
		});

		List<Class<? extends Gamer>> gamersCopy = new ArrayList<Class<? extends Gamer>>(gamers);
		for(Class<? extends Gamer> gamer : gamersCopy)
		{
			Gamer g;
			try {
				g = gamer.newInstance();
				typeComboBox.addItem(g.getName());
			} catch(Exception ex) {
			    gamers.remove(gamer);
			}
		}
		typeComboBox.setSelectedItem("Random");

		JPanel managerPanel = new JPanel(new GridBagLayout());
		managerPanel.setBorder(new TitledBorder("Manager"));

		managerPanel.add(new JLabel("Port:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(20, 5, 5, 5), 5, 5));
		managerPanel.add(portTextField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Type:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(typeComboBox, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(createButton, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel playersPanel = new JPanel(new GridBagLayout());
		playersPanel.setBorder(new TitledBorder("Players"));

		playersPanel.add(playersTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

		this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(playersPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	private AbstractAction createButtonMethod()
	{
		return new AbstractAction("Create")
		{

			@Override
			public void actionPerformed(ActionEvent evt)
			{
				try
				{
					int port = Integer.valueOf(portTextField.getText());
					String type = (String) typeComboBox.getSelectedItem();

					MatchPanel matchPanel = new MatchPanel();
					NetworkPanel networkPanel = new NetworkPanel();
					DetailPanel detailPanel = null;
					ConfigPanel configPanel = null;
					Gamer gamer = null;

					Class<?> gamerClass = gamers.get(typeComboBox.getSelectedIndex());
					try {
						gamer = (Gamer) gamerClass.newInstance();
					} catch(Exception ex) { throw new RuntimeException(ex); }
					detailPanel = gamer.getDetailPanel();
					configPanel = gamer.getConfigPanel();

					gamer.addObserver(matchPanel);
					gamer.addObserver(detailPanel);

					GamePlayer player = new GamePlayer(port, gamer);
					player.addObserver(networkPanel);
					player.start();

					JTabbedPane tab = new JTabbedPane();
					tab.addTab("Match", matchPanel);
					tab.addTab("Network", networkPanel);
					tab.addTab("Configuration", configPanel);
					tab.addTab("Detail", detailPanel);
					playersTabbedPane.addTab(type + " (" + player.getGamerPort() + ")", tab);
					playersTabbedPane.setSelectedIndex(playersTabbedPane.getTabCount()-1);

					defaultPort++;
					portTextField.setText(defaultPort.toString());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
	}
}