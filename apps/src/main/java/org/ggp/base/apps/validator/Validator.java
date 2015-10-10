package org.ggp.base.apps.validator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.ui.GameSelector;
import org.ggp.base.util.ui.NativeUI;
import org.ggp.base.validator.BasesInputsValidator;
import org.ggp.base.validator.GameValidator;
import org.ggp.base.validator.OPNFValidator;
import org.ggp.base.validator.SimulationValidator;
import org.ggp.base.validator.StaticValidator;

@SuppressWarnings("serial")
public final class Validator extends JPanel implements ActionListener
{
	private static void createAndShowGUI(Validator validatorPanel)
	{
	    JFrame frame = new JFrame("Gdl Validator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setPreferredSize(new Dimension(1024, 768));
		frame.getContentPane().add(validatorPanel);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args)
	{
	    NativeUI.setNativeUI();
		final Validator validatorPanel = new Validator();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				createAndShowGUI(validatorPanel);
			}
		});
	}

	private Game theGame;
	private final JButton validateButton;
	private final JTextField maxDepthTextField;
	private final JTextField simulationsTextField;
	private final JTextField millisToSimulateField;
	private final JTabbedPane simulationsTabbedPane;

    private final GameSelector gameSelector;

	public Validator()
	{
	    super(new GridBagLayout());

		validateButton = new JButton(validateButtonMethod(this));
		maxDepthTextField = new JTextField("100");
		simulationsTextField = new JTextField("10");
		millisToSimulateField = new JTextField("5000");
		simulationsTabbedPane = new JTabbedPane();

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Simulation");
		model.addColumn("Result");

		maxDepthTextField.setColumns(15);
		simulationsTextField.setColumns(15);
		validateButton.setEnabled(false);

        gameSelector = new GameSelector();

		int nRowCount = 0;
		JPanel sourcePanel = new JPanel(new GridBagLayout());
		sourcePanel.setBorder(new TitledBorder("Source"));
		sourcePanel.add(new JLabel("Repository:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        sourcePanel.add(gameSelector.getRepositoryList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        sourcePanel.add(new JLabel("Game:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        sourcePanel.add(gameSelector.getGameList(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        sourcePanel.add(new JSeparator(), new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(new JLabel("Step Limit:"), new GridBagConstraints(0, nRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(maxDepthTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(new JLabel("Simulations:"), new GridBagConstraints(0, nRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(simulationsTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(new JLabel("Base Sim ms:"), new GridBagConstraints(0, nRowCount, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(millisToSimulateField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(validateButton, new GridBagConstraints(1, nRowCount++, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel simulationsPanel = new JPanel(new GridBagLayout());
		simulationsPanel.setBorder(new TitledBorder("Results"));
		simulationsPanel.add(simulationsTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		this.add(sourcePanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(simulationsPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

        gameSelector.getGameList().addActionListener(this);
        gameSelector.repopulateGameList();
	}

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == gameSelector.getGameList()) {
            theGame = gameSelector.getSelectedGame();
            validateButton.setEnabled(theGame != null);
        }
    }

	private AbstractAction validateButtonMethod(final Validator validatorPanel)
	{
		return new AbstractAction("Validate")
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				try {
					int maxDepth = Integer.valueOf(maxDepthTextField.getText());
					int simulations = Integer.valueOf(simulationsTextField.getText());
					int millisToSimulate = Integer.valueOf(millisToSimulateField.getText());

					GameValidator[] theValidators = new GameValidator[] {
							new OPNFValidator(),
							new SimulationValidator(maxDepth, simulations),
							new BasesInputsValidator(millisToSimulate),
							new StaticValidator(),
					};
					OutcomePanel simulationPanel = new OutcomePanel(theValidators.length);
					for (GameValidator theValidator : theValidators) {
						ValidatorThread validator = new ValidatorThread(theGame, theValidator);
						validator.addObserver(simulationPanel);
						validator.start();
					}

					validatorPanel.simulationsTabbedPane.addTab(theGame.getKey(), simulationPanel);
				} catch (Exception e) {
					// Do nothing.
				}
			}
		};
	}
}
