package org.ggp.base.apps.player.match;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.player.gamer.event.GamerCompletedMatchEvent;
import org.ggp.base.player.gamer.event.GamerNewMatchEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.table.JZebraTable;


@SuppressWarnings("serial")
public final class MatchPanel extends JPanel implements Observer
{

	private final JZebraTable matchTable;

	public MatchPanel()
	{
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Match Id");
		model.addColumn("Role");
		model.addColumn("Start Clock");
		model.addColumn("Play Clock");
		model.addColumn("Status");

		matchTable = new JZebraTable(model)
		{

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		matchTable.setShowHorizontalLines(true);
		matchTable.setShowVerticalLines(true);

		this.add(new JScrollPane(matchTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	@Override
	public void observe(Event event)
	{
		if (event instanceof GamerCompletedMatchEvent)
		{
			observe((GamerCompletedMatchEvent) event);
		}
		else if (event instanceof GamerNewMatchEvent)
		{
			observe((GamerNewMatchEvent) event);
		}
	}

	private void observe(GamerCompletedMatchEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) matchTable.getModel();
		model.setValueAt("Inactive", model.getRowCount() - 1, 4);
	}

	private void observe(GamerNewMatchEvent event)
	{
		String matchId = event.getMatch().getMatchId();
		String role = event.getRoleName().toString();
		String startClock = Integer.toString(event.getMatch().getStartClock());
		String playClock = Integer.toString(event.getMatch().getPlayClock());
		String status = "Active";

		DefaultTableModel model = (DefaultTableModel) matchTable.getModel();
		model.addRow(new String[] { matchId, role, startClock, playClock, status });
	}

}
