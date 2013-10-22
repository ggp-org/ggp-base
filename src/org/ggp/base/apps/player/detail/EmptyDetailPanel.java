package org.ggp.base.apps.player.detail;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import org.ggp.base.util.observer.Event;

/**
 * This is a detail panel that contains no information at all.
 */
@SuppressWarnings("serial")
public class EmptyDetailPanel extends DetailPanel {

	public EmptyDetailPanel() {
		super(new GridBagLayout());
		this.add(new JLabel("No details available."), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
	}

	public void observe(Event event) {
		// Do nothing when notified about events
	}
}
