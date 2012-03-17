package apps.player.network;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import player.event.PlayerReceivedMessageEvent;
import player.event.PlayerSentMessageEvent;
import player.gamer.event.GamerNewMatchEvent;
import util.observer.Event;
import util.observer.Observer;
import util.ui.table.JZebraTable;

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
	}

}
