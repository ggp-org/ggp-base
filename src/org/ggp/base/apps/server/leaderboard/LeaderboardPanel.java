package org.ggp.base.apps.server.leaderboard;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.ggp.base.server.event.ServerMatchUpdatedEvent;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.JLabelBold;

@SuppressWarnings("serial")
public final class LeaderboardPanel extends JPanel implements Observer
{
	private final JTable leaderTable;
	private final TableRowSorter<TableModel> sorter;

	public LeaderboardPanel()
	{
		super(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Player");
        model.addColumn("Score");
        model.addColumn("Errors");

		leaderTable = new JTable(model)
		{
			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
			@Override
			public Class<?> getColumnClass(int colIndex) {
				if (colIndex == 0) return String.class;
				if (colIndex == 1) return Integer.class;
				if (colIndex == 2) return Integer.class;
				return Object.class;
			}
		};
		leaderTable.setShowHorizontalLines(true);
		leaderTable.setShowVerticalLines(true);
		leaderTable.getColumnModel().getColumn(0).setPreferredWidth(40);
		leaderTable.getColumnModel().getColumn(1).setPreferredWidth(10);
		leaderTable.getColumnModel().getColumn(2).setPreferredWidth(10);
		sorter = new TableRowSorter<TableModel>(model);
		sorter.setComparator(1, new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return a-b;
			}
		});
		sorter.setSortKeys(Arrays.asList(new SortKey[]{new SortKey(1, SortOrder.DESCENDING)}));
		leaderTable.setRowSorter(sorter);

		add(new JLabelBold("Leaderboard"), BorderLayout.NORTH);
		add(new JScrollPane(leaderTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
	}

	@Override
	public void observe(Event event)
	{
		if (!(event instanceof ServerMatchUpdatedEvent)) return;
		Match match = ((ServerMatchUpdatedEvent) event).getMatch();

		if (!match.isCompleted()) return;
		if (match.getMatchId().startsWith("Test")) return;

		List<Integer> goals = match.getGoalValues();
		List<Integer> errors = getErrorCounts(match.getErrorHistory());
		List<String> players = match.getPlayerNamesFromHost();
		for (int i = 0; i < players.size(); i++) { if (players.get(i)==null) { players.set(i, "?"); } }

		Set<String> playersToAdd = new HashSet<String>(players);
		DefaultTableModel model = (DefaultTableModel) leaderTable.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String rowPlayer = model.getValueAt(i, 0).toString();
			int playerIndex = players.indexOf(rowPlayer);
			if (playerIndex != -1) {
				int oldScore = (Integer)model.getValueAt(i, 1);
				int oldErrors = (Integer)model.getValueAt(i, 2);
				model.setValueAt(oldScore + goals.get(playerIndex), i, 1);
				model.setValueAt(oldErrors + errors.get(playerIndex), i, 2);
				playersToAdd.remove(rowPlayer);
			}
		}
		for (String playerToAdd : playersToAdd) {
			model.addRow(new Object[]{playerToAdd, goals.get(players.indexOf(playerToAdd)), errors.get(players.indexOf(playerToAdd))});
		}
		sorter.sort();
	}

	public static List<Integer> getErrorCounts(List<List<String>> errorHistory) {
		List<Integer> errorCounts = new ArrayList<Integer>();
		for (int i = 0; i < errorHistory.get(0).size(); i++) {
			errorCounts.add(0);
		}
		for (List<String> errorHistoryEntry : errorHistory) {
			for (int i = 0; i < errorHistoryEntry.size(); i++) {
				if (!errorHistoryEntry.get(i).isEmpty()) {
					errorCounts.set(i,1+errorCounts.get(i));
				}
			}
		}
		return errorCounts;
	}
}
