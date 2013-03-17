package org.ggp.base.apps.server.scheduling;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.server.event.ServerMatchUpdatedEvent;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.JLabelBold;

@SuppressWarnings("serial")
public final class SchedulingPanel extends JPanel implements Observer
{
	private final JTable queueTable;
	
	public SchedulingPanel()
	{
		super(new BorderLayout());
		
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID");
        model.addColumn("Game");
        model.addColumn("Clock");
        model.addColumn("Status");
        model.addColumn("Players");
        model.addColumn("Goals");
        model.addColumn("Errors");
        model.addColumn("Step");
        
        queueTable = new JTable(model)
		{
			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		queueTable.setShowHorizontalLines(true);
		queueTable.setShowVerticalLines(true);
		queueTable.getColumnModel().getColumn(0).setPreferredWidth(1);
		queueTable.getColumnModel().getColumn(1).setPreferredWidth(60);
		queueTable.getColumnModel().getColumn(2).setPreferredWidth(40);
		queueTable.getColumnModel().getColumn(3).setPreferredWidth(50);
		queueTable.getColumnModel().getColumn(4).setPreferredWidth(150);
		queueTable.getColumnModel().getColumn(5).setPreferredWidth(40);
		queueTable.getColumnModel().getColumn(6).setPreferredWidth(45);
		queueTable.getColumnModel().getColumn(7).setPreferredWidth(40);

		add(new JLabelBold("Scheduling Queue"), BorderLayout.NORTH);
		add(new JScrollPane(queueTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
	}
	
	public void observe(Event event)
	{
		if (!(event instanceof ServerMatchUpdatedEvent)) return;		
		Match match = ((ServerMatchUpdatedEvent) event).getMatch();
		
		DefaultTableModel model = (DefaultTableModel) queueTable.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String rowMatchId = model.getValueAt(i, 0).toString();
			if (rowMatchId.equals(match.getMatchId())) {
				String status = "active";
				if (match.isAborted()) status = "aborted";
				if (match.isCompleted()) status = "done";
				model.setValueAt(status, i, 3);
				if (match.isCompleted()) {
					model.setValueAt(getLinebreakString(match.getGoalValues()), i, 5);					
				}
				List<Integer> errorCounts = new ArrayList<Integer>();
				for (int j = 0; j < match.getPlayerNamesFromHost().size(); j++) {
					errorCounts.add(0);
				}
				for (List<String> errors : match.getErrorHistory()) {
					for (int j = 0; j < errors.size(); j++) {
						if (!errors.get(j).isEmpty()) {
							errorCounts.set(j, errorCounts.get(j) + 1);
						}
					}
				}
				model.setValueAt(getLinebreakString(errorCounts), i, 6);
				model.setValueAt(match.getStateHistory().size(), i, 7);
				return;
			}
		}

		// Couldn't find the match in the existing list -- add it.
		model.addRow(new Object[]{match.getMatchId(),match.getGame().getKey(),match.getStartClock() + "," + match.getPlayClock(),"pending",getLinebreakString(match.getPlayerNamesFromHost()),"","",1});
		queueTable.setRowHeight(model.getRowCount()-1, match.getPlayerNamesFromHost().size()*20);
	}
	
	private static String getLinebreakString(List<?> objects) {
		String renderedString = "<html>";
		for (Object object : objects) {
			renderedString += (object == null ? "?" : object.toString()) + "<br>";
		}
		return renderedString.substring(0, renderedString.length()-"<br>".length())+"</html>";
	}
}