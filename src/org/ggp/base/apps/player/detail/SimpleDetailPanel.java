package org.ggp.base.apps.player.detail;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.player.gamer.event.GamerNewMatchEvent;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.ui.table.JZebraTable;

/**
 * This is a simple tabular detail panel that shows the number of
 * available moves, the time it took to select a move, and the move
 * that was selected. 
 */
@SuppressWarnings("serial")
public class SimpleDetailPanel extends DetailPanel {
	private final JZebraTable moveTable;

	public SimpleDetailPanel() {
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Available Moves");
		model.addColumn("Computation Time");
		model.addColumn("Selected Move");

		moveTable = new JZebraTable(model) {
			@Override
			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false;
			}
		};
		moveTable.setShowHorizontalLines(true);
		moveTable.setShowVerticalLines(true);

		this.add(new JScrollPane(moveTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	public void observe(Event event) {
		if (event instanceof GamerNewMatchEvent) {
			observe((GamerNewMatchEvent) event);
		} else if (event instanceof GamerSelectedMoveEvent) {
			observe((GamerSelectedMoveEvent) event);
		}
	}

	private void observe(GamerNewMatchEvent event) {
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.setRowCount(0);
	}

	private void observe(GamerSelectedMoveEvent event) {
		String availableMoves = Integer.toString(event.getMoves().size());
		String computationTime = Long.toString(event.getTime()) + " ms";
		String move = event.getSelection().toString();

		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.addRow(new String[] { availableMoves, computationTime, move });
	}
}