package player.gamer.statemachine.reflex.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import player.gamer.event.GamerNewMatchEvent;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import util.observer.Event;
import util.ui.table.JZebraTable;
import apps.player.detail.DetailPanel;

@SuppressWarnings("serial")
public final class ReflexDetailPanel extends DetailPanel
{
	private final JZebraTable moveTable;

	public ReflexDetailPanel()
	{
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Move Count");
		model.addColumn("Computation Time");
		model.addColumn("mps");
		model.addColumn("Move");

		moveTable = new JZebraTable(model)
		{

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		moveTable.setShowHorizontalLines(true);
		moveTable.setShowVerticalLines(true);

		this.add(new JScrollPane(moveTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	public void observe(Event event)
	{
		if (event instanceof GamerNewMatchEvent)
		{
			observe((GamerNewMatchEvent) event);
		}
		else if (event instanceof ReflexMoveSelectionEvent)
		{
			observe((ReflexMoveSelectionEvent) event);
		}
	}

	private void observe(GamerNewMatchEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.setRowCount(0);
	}

	private void observe(ReflexMoveSelectionEvent event)
	{
		String moveCount = Integer.toString(event.getMoves().size());
		String computationTime = Long.toString(event.getTime()) + " ms";
		String mps = Long.toString(1000 * event.getMoves().size() / Math.max(1, event.getTime()));
		String move = event.getSelection().toString();

		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.addRow(new String[] { moveCount, computationTime, mps, move });
	}
}
