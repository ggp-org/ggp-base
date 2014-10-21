package org.ggp.base.apps.player.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

@SuppressWarnings("serial")
public final class EmptyConfigPanel extends ConfigPanel
{

	public EmptyConfigPanel()
	{
		super(new GridBagLayout());

		this.add(new JLabel("No options available."), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
	}

}
