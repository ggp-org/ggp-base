package org.ggp.base.apps.server.scheduling;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.server.event.ServerMatchUpdatedEvent;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.presence.PlayerPresence;
import org.ggp.base.util.ui.JLabelBold;

@SuppressWarnings("serial")
public final class SchedulingPanel extends JPanel implements Observer, ListSelectionListener, TableModelListener
{
	private final JTable queueTable;

	private final JButton viewSaved;
	private final JButton viewPublished;
	private final JButton remove;

	// Track the external filenames and URLs for each match, so that they can
	// be opened for viewing as needed.
	private final Map<String,String> matchIdToURL = new HashMap<String,String>();
	private final Map<String,String> matchIdToFilename = new HashMap<String,String>();

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
		queueTable.getColumnModel().getColumn(3).setPreferredWidth(55);
		queueTable.getColumnModel().getColumn(4).setPreferredWidth(145);
		queueTable.getColumnModel().getColumn(5).setPreferredWidth(40);
		queueTable.getColumnModel().getColumn(6).setPreferredWidth(45);
		queueTable.getColumnModel().getColumn(7).setPreferredWidth(40);
		queueTable.getSelectionModel().addListSelectionListener(this);
		queueTable.getModel().addTableModelListener(this);

		JPanel buttonPanel = new JPanel();
		viewSaved = new JButton(viewSavedMatchButtonMethod());
		viewSaved.setEnabled(false);
		viewPublished = new JButton(viewPublishedMatchButtonMethod());
		viewPublished.setEnabled(false);
		remove = new JButton(removeMatchButtonMethod());
		remove.setEnabled(false);
		buttonPanel.add(viewSaved);
		buttonPanel.add(viewPublished);
		buttonPanel.add(remove);

		add(new JLabelBold("Scheduling Queue"), BorderLayout.NORTH);
		add(new JScrollPane(queueTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private Scheduler scheduler;
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	private AbstractAction viewPublishedMatchButtonMethod() {
		return new AbstractAction("View Published") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (queueTable.getSelectedRow() >= 0) {
					String matchId = queueTable.getModel().getValueAt(queueTable.getSelectedRow(), 0).toString();
					if (matchIdToURL.containsKey(matchId)) {
						try {
							java.awt.Desktop.getDesktop().browse(java.net.URI.create(matchIdToURL.get(matchId)));
						} catch (IOException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		};
	}

	private AbstractAction viewSavedMatchButtonMethod() {
		return new AbstractAction("View Saved") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (queueTable.getSelectedRow() >= 0) {
					String matchId = queueTable.getModel().getValueAt(queueTable.getSelectedRow(), 0).toString();
					if (matchIdToFilename.containsKey(matchId)) {
						try {
							java.awt.Desktop.getDesktop().browse(java.net.URI.create("file://" + matchIdToFilename.get(matchId)));
						} catch (IOException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		};
	}

	private AbstractAction removeMatchButtonMethod() {
		return new AbstractAction("Cancel Match") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (queueTable.getSelectedRow() >= 0) {
					String matchId = queueTable.getModel().getValueAt(queueTable.getSelectedRow(), 0).toString();
					String state = queueTable.getModel().getValueAt(queueTable.getSelectedRow(), 3).toString();
					if (state.equals("pending")) {
						;
					} else if (state.equals("active")) {
						if (scheduler != null) {
							scheduler.abortOngoingMatch(matchId);
						}
					} else if (state.equals("aborted") || state.equals("done")) {
						int[] nRows = queueTable.getSelectedRows();
						queueTable.clearSelection();
						Arrays.sort(nRows);
						for (int i = nRows.length-1; i >= 0; i--) {
							((DefaultTableModel)queueTable.getModel()).removeRow(nRows[i]);
						}
					}
				}
			}
		};
	}

	@Override
	public void tableChanged(TableModelEvent arg0) {
		updateButtonState();
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		updateButtonState();
	}

	private void updateButtonState() {
		if (queueTable.getSelectedRow() >= 0) {
			String matchId = queueTable.getModel().getValueAt(queueTable.getSelectedRow(), 0).toString();
			String state = queueTable.getModel().getValueAt(queueTable.getSelectedRow(), 3).toString();
			viewSaved.setEnabled(matchIdToFilename.containsKey(matchId));
			viewPublished.setEnabled(matchIdToURL.containsKey(matchId));
			if (state.equals("pending")) {
				remove.setEnabled(false);
				// TODO: Add support for canceling pending matches.
			} else if (state.equals("active")) {
				remove.setText("Cancel Match");
				remove.setEnabled(true);
			} else if (state.equals("aborted") || state.equals("done")) {
				remove.setText("Hide Match");
				remove.setEnabled(true);
			}
		} else {
			viewSaved.setEnabled(false);
			viewPublished.setEnabled(false);
			remove.setEnabled(false);
		}
	}

	public void addPendingMatch(PendingMatch spec) {
		DefaultTableModel model = (DefaultTableModel) queueTable.getModel();
		model.addRow(new Object[]{spec.matchID,spec.theGame.getKey(),spec.startClock + "," + spec.playClock,"pending",getLinebreakString(getNamesForPlayers(spec.thePlayers)),"","",0});
		queueTable.setRowHeight(model.getRowCount()-1, spec.thePlayers.size()*20);
	}

	@Override
	public void observe(Event genericEvent) {
		if (!(genericEvent instanceof ServerMatchUpdatedEvent)) return;
		ServerMatchUpdatedEvent event = (ServerMatchUpdatedEvent)genericEvent;
		Match match = event.getMatch();

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
				List<String> errorCountStrings = new ArrayList<String>();
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
				for (int errorCount : errorCounts) {
					if (errorCount > 0) {
						errorCountStrings.add("<font color=red>" + errorCount + "</font>");
					} else {
						errorCountStrings.add("0");
					}
				}
				model.setValueAt(getLinebreakString(errorCountStrings), i, 6);
				model.setValueAt(match.getStateHistory().size()-1, i, 7);

				if (event.getExternalPublicationKey() != null) {
					matchIdToURL.put(match.getMatchId(), "http://www.ggp.org/view/all/matches/" + event.getExternalPublicationKey() + "/");
				}
				if (event.getExternalFilename() != null) {
					matchIdToFilename.put(match.getMatchId(), event.getExternalFilename());
				}

				return;
			}
		}

		// Couldn't find the match in the existing list -- add it.
		model.addRow(new Object[]{match.getMatchId(),match.getGame().getKey(),match.getStartClock() + "," + match.getPlayClock(),"starting",getLinebreakString(match.getPlayerNamesFromHost()),"","",0});
		queueTable.setRowHeight(model.getRowCount()-1, match.getPlayerNamesFromHost().size()*20);
	}

	private static List<String> getNamesForPlayers(List<PlayerPresence> players) {
		List<String> playerNames = new ArrayList<String>();
		for (PlayerPresence player : players) {
			playerNames.add(player.getName());
		}
		return playerNames;
	}

	private static String getLinebreakString(List<?> objects) {
		String renderedString = "<html>";
		for (Object object : objects) {
			renderedString += (object == null ? "?" : object.toString()) + "<br>";
		}
		return renderedString.substring(0, renderedString.length()-"<br>".length())+"</html>";
	}
}