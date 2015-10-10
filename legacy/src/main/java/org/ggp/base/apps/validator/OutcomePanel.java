package org.ggp.base.apps.validator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.apps.validator.event.ValidatorFailureEvent;
import org.ggp.base.apps.validator.event.ValidatorSuccessEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.table.JZebraTable;
import org.ggp.base.validator.ValidatorWarning;

@SuppressWarnings("serial")
public final class OutcomePanel extends JPanel implements Observer
{

	private final JZebraTable logTable;
	private final JProgressBar progressBar;

	public OutcomePanel(int numValidators)
	{
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Validator");
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

		logTable.setRowHeight(100);
		logTable.setShowHorizontalLines(true);
		logTable.setShowVerticalLines(true);
		logTable.getColumnModel().getColumn(0).setMaxWidth(150);
		logTable.getColumnModel().getColumn(0).setPreferredWidth(500);
		progressBar.setMaximum(numValidators);

		this.add(new JScrollPane(logTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(progressBar, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	@Override
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

	public static final String wrapLine(String line, int width) {
		StringBuilder wrappedLine = new StringBuilder();
		while (line.length() > width) {
			wrappedLine.append(line.substring(0, width) + "<br>");
			line = line.substring(width);
		}
		wrappedLine.append(line);
		return "<html>" + wrappedLine.toString() + "</html>";
	}

	private void observeValidationFailureEvent(ValidatorFailureEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) logTable.getModel();
		int numRows = model.getRowCount() + 1;

		model.addRow(new String[] { event.getName(), wrapLine(event.getException().toString(), 100) });
		progressBar.setValue(numRows);
	}

	private void observeValidationSuccessEvent(ValidatorSuccessEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) logTable.getModel();
		int numRows = model.getRowCount() + 1;

		List<ValidatorWarning> warnings = event.getWarnings();
		if (warnings.isEmpty()) {
			model.addRow(new String[] { event.getName(), "Success!" });
		} else {
			model.addRow(new String[] { event.getName(), wrapLine("Success, with warnings: " + warnings, 100) });
		}
		progressBar.setValue(numRows);
	}

}
