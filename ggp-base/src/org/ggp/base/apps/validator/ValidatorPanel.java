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

import org.ggp.base.apps.validator.simulation.SimulationPanel;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.ui.GameSelector;
import org.ggp.base.util.ui.NativeUI;
import org.ggp.base.validator.GdlValidator;


@SuppressWarnings("serial")
public final class ValidatorPanel extends JPanel implements ActionListener
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

	private Game theGame;
	private final JButton stepByHandButton;
	private final JTextField maxDepthTextField;
	private final JTabbedPane simulationsTabbedPane;
	private final JTextField simulationsTextField;
	private final JButton validateButton;

    private final GameSelector gameSelector;	
	
	public ValidatorPanel()
	{
	    super(new GridBagLayout());

		validateButton = new JButton(validateButtonMethod(this));
		stepByHandButton = new JButton(stepByHandButtonMethod(this));
		maxDepthTextField = new JTextField("100");
		simulationsTextField = new JTextField("10");
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
		sourcePanel.add(stepByHandButton, new GridBagConstraints(1, nRowCount++, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        sourcePanel.add(validateButton, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		

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

					GdlValidator validator = new GdlValidator(theGame.getRules(), maxDepth, simulations);
					validator.addObserver(simulationPanel);

					validatorPanel.simulationsTabbedPane.addTab(theGame.getKey(), simulationPanel);
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
					validatorPanel.simulationsTabbedPane.addTab(theGame.getKey()+" Stepper", QP);
				} catch (Exception e) {
					// Do nothing.
				}
			}
		};
	}
}
