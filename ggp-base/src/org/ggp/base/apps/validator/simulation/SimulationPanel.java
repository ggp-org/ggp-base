package org.ggp.base.apps.validator.simulation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.table.JZebraTable;
import org.ggp.base.validator.event.ValidatorFailureEvent;
import org.ggp.base.validator.event.ValidatorSuccessEvent;


@SuppressWarnings("serial")
public final class SimulationPanel extends JPanel implements Observer
{

	private final JZebraTable logTable;
	private final JProgressBar progressBar;

	public SimulationPanel(int numSimulations)
	{
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Simulation");
		model.addColumn("Result");

		logTable = new JZebraTable(model)
		{

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		progressBar = new JProgressBar();

		logTable.setShowHorizontalLines(true);
		logTable.setShowVerticalLines(true);
		progressBar.setMaximum(numSimulations);

		this.add(new JScrollPane(logTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(progressBar, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	public void observe(Event event)
	{
		if (event instanceof ValidatorSuccessEvent)
		{
			observeValidationSuccessEvent((ValidatorSuccessEvent) event);
		}
		else if (event instanceof ValidatorFailureEvent)
		{
			observeValidationFailureEvent((ValidatorFailureEvent) event);
		}
	}

	private void observeValidationFailureEvent(ValidatorFailureEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) logTable.getModel();
		int numRows = model.getRowCount() + 1;

		model.addRow(new String[] { Integer.toString(numRows), event.getException().toString() });
		progressBar.setValue(numRows);
	}

	private void observeValidationSuccessEvent(ValidatorSuccessEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) logTable.getModel();
		int numRows = model.getRowCount() + 1;

		model.addRow(new String[] { Integer.toString(numRows), "Success!" });
		progressBar.setValue(numRows);
	}

}
