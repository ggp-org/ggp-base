package org.ggp.base.apps.server.publishing;

import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.ggp.base.server.GameServer;
import org.ggp.base.util.ui.PublishButton;


@SuppressWarnings("serial")
public final class PublishingPanel extends JPanel
{
	public PublishingPanel(GameServer theServer)
	{
		super(new GridBagLayout());
		
		PublishButton theButton = new PublishButton("Publish online!");
		theButton.setServer(theServer);
		add(theButton);
	}
}