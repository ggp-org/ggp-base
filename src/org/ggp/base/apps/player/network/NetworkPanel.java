package org.ggp.base.apps.player.network;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.player.event.PlayerReceivedMessageEvent;
import org.ggp.base.player.event.PlayerSentMessageEvent;
import org.ggp.base.player.gamer.event.GamerNewMatchEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.table.JZebraTable;


@SuppressWarnings("serial")
public final class NetworkPanel extends JPanel implements Observer
{
	private final JZebraTable networkTable;

	public NetworkPanel()
	{
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Received");
		model.addColumn("Sent");

		networkTable = new JZebraTable(model)
		{
			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		networkTable.setShowHorizontalLines(true);
		networkTable.setShowVerticalLines(true);

		this.add(new JScrollPane(networkTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	@Override
	public void observe(Event event)
	{
		if (event instanceof PlayerReceivedMessageEvent)
		{
			observe((PlayerReceivedMessageEvent) event);
		}
		else if (event instanceof PlayerSentMessageEvent)
		{
			observe((PlayerSentMessageEvent) event);
		}
		else if (event instanceof GamerNewMatchEvent)
		{
			observe(event);
		}
	}

	private void observe(PlayerReceivedMessageEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) networkTable.getModel();
		model.addRow(new String[] { "", "" });
		model.setValueAt(event.getMessage(), model.getRowCount() - 1, 0);
	}

	private void observe(PlayerSentMessageEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) networkTable.getModel();
		model.setValueAt(event.getMessage(), model.getRowCount() - 1, 1);
		if (model.getValueAt(model.getRowCount()-1, 0).toString().toLowerCase().equals("( info )")) {
			// When we're observing info requests and responses, don't bother displaying them,
			// since they happen frequently and don't convey much interesting information. This
			// improves the signal-to-noise ratio so the player's actual moves are visible here.
			model.removeRow(model.getRowCount() - 1);
		}
	}

}
