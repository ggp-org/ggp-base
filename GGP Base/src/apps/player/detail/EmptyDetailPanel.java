package apps.player.detail;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

import util.observer.Event;

@SuppressWarnings("serial")
public class EmptyDetailPanel extends DetailPanel {

	public EmptyDetailPanel() {
		super(new GridBagLayout());

		this.add(new JLabel("No details available."), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
	}

	public void observe(Event event) {
				
	}

}
