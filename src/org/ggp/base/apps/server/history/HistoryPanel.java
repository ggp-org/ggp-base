package org.ggp.base.apps.server.history;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.server.event.ServerCompletedMatchEvent;
import org.ggp.base.server.event.ServerNewMatchEvent;
import org.ggp.base.server.event.ServerNewMovesEvent;
import org.ggp.base.server.event.ServerTimeEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.ui.table.JZebraTable;
import org.ggp.base.util.ui.timer.JTimerBar;


@SuppressWarnings("serial")
public final class HistoryPanel extends JPanel implements Observer
{

	private final JZebraTable historyTable;
	private final JTimerBar timerBar;

	public HistoryPanel()
	{
		super(new GridBagLayout());

		historyTable = new JZebraTable(new DefaultTableModel())
		{

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		timerBar = new JTimerBar();

		historyTable.setShowHorizontalLines(true);
		historyTable.setShowVerticalLines(true);

		this.add(new JScrollPane(historyTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(timerBar, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	@Override
	public void observe(Event event)
	{
		if (event instanceof ServerNewMatchEvent)
		{
			observe((ServerNewMatchEvent) event);
		}
		else if (event instanceof ServerNewMovesEvent)
		{
			observe((ServerNewMovesEvent) event);
		}
		else if (event instanceof ServerCompletedMatchEvent)
		{
			observe((ServerCompletedMatchEvent) event);
		}
		else if (event instanceof ServerTimeEvent)
		{
			observe((ServerTimeEvent) event);
		}
	}

	private void observe(ServerCompletedMatchEvent event)
	{
		timerBar.stop();

		String[] row = new String[event.getGoals().size() + 1];
		row[0] = "Terminal";
		for (int i = 0; i < event.getGoals().size(); i++)
		{
			row[i + 1] = Integer.toString(event.getGoals().get(i));
		}

		DefaultTableModel model = (DefaultTableModel) historyTable.getModel();
		model.addRow(row);
	}

	private void observe(ServerNewMatchEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) historyTable.getModel();

		model.addColumn("Step");
		for (Role role : event.getRoles())
		{
			model.addColumn(role.toString());
		}
	}

	private void observe(ServerNewMovesEvent event)
	{
		String[] row = new String[event.getMoves().size() + 1];
		row[0] = Integer.toString(historyTable.getRowCount() + 1);
		for (int i = 0; i < event.getMoves().size(); i++)
		{
			row[i + 1] = event.getMoves().get(i).toString();
		}

		DefaultTableModel model = (DefaultTableModel) historyTable.getModel();
		model.addRow(row);
	}

	private void observe(ServerTimeEvent event)
	{
		timerBar.time(event.getTime(), 500);
	}
}
