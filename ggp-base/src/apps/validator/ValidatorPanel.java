package apps.validator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import util.game.Game;
import validator.GdlValidator;
import apps.common.GameLoaderPrompt;
import apps.common.NativeUI;
import apps.validator.simulation.SimulationPanel;

@SuppressWarnings("serial")
public final class ValidatorPanel extends JPanel
{
	private static void createAndShowGUI(ValidatorPanel validatorPanel)
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
		final ValidatorPanel validatorPanel = new ValidatorPanel();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				createAndShowGUI(validatorPanel);
			}
		});
	}

	private Game game;
	private final JButton fileButton;
	private final JButton stepByHandButton;
	private final JTextField fileTextField;
	private final JTextField maxDepthTextField;
	private final JTabbedPane simulationsTabbedPane;
	private final JTextField simulationsTextField;
	private final JButton validateButton;

	public ValidatorPanel()
	{
	    super(new GridBagLayout());

		fileButton = new JButton(selectButtonMethod());
		validateButton = new JButton(validateButtonMethod(this));
		stepByHandButton = new JButton(stepByHandButtonMethod(this));
		fileTextField = new JTextField("Select a .kif file");
		maxDepthTextField = new JTextField("100");
		simulationsTextField = new JTextField("10");
		simulationsTabbedPane = new JTabbedPane();

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Simulation");
		model.addColumn("Result");

		fileTextField.setEnabled(false);
		fileTextField.setColumns(15);
		maxDepthTextField.setColumns(15);
		simulationsTextField.setColumns(15);
		validateButton.setEnabled(false);

		JPanel sourcePanel = new JPanel(new GridBagLayout());
		sourcePanel.setBorder(new TitledBorder("Source"));
		sourcePanel.add(fileButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		sourcePanel.add(fileTextField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(new JLabel("Step Limit:"), new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(maxDepthTextField, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(new JLabel("Simulations:"), new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(simulationsTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		sourcePanel.add(validateButton, new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		sourcePanel.add(stepByHandButton, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel simulationsPanel = new JPanel(new GridBagLayout());
		simulationsPanel.setBorder(new TitledBorder("Results"));
		simulationsPanel.add(simulationsTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		this.add(sourcePanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(simulationsPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	private AbstractAction selectButtonMethod()
	{
		return new AbstractAction("Source File")
		{
			public void actionPerformed(ActionEvent evt)
			{
	           Game theGame = GameLoaderPrompt.loadGameUsingPrompt();
	           if (theGame == null) return;
	           
	           game = theGame;
	           fileTextField.setText(theGame.getKey());
	           validateButton.setEnabled(true);	           
			}
		};
	}

	private AbstractAction validateButtonMethod(final ValidatorPanel validatorPanel)
	{
		return new AbstractAction("Validate")
		{
			public void actionPerformed(ActionEvent evt)
			{
				try {
					int maxDepth = Integer.valueOf(maxDepthTextField.getText());
					int simulations = Integer.valueOf(simulationsTextField.getText());

					SimulationPanel simulationPanel = new SimulationPanel(simulations);

					GdlValidator validator = new GdlValidator(game.getRules(), maxDepth, simulations);
					validator.addObserver(simulationPanel);

					validatorPanel.simulationsTabbedPane.addTab(validatorPanel.fileTextField.getText(), simulationPanel);
					validator.start();
				} catch (Exception e) {
					// Do nothing.
				}
			}
		};
	}
	
	private AbstractAction stepByHandButtonMethod(final ValidatorPanel validatorPanel)
	{
		return new AbstractAction("Step By Hand")
		{
			public void actionPerformed(ActionEvent evt)
			{
				try {
					QueryPanel QP = new QueryPanel();
					validatorPanel.simulationsTabbedPane.addTab(validatorPanel.fileTextField.getText()+" Stepper", QP);
				} catch (Exception e) {
					// Do nothing.
				}
			}
		};
	}
}
