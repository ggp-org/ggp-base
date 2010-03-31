package apps.player.detail;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import util.observer.Observer;

@SuppressWarnings("serial")
public abstract class DetailPanel extends JPanel implements Observer
{

	public DetailPanel(LayoutManager layoutManager)
	{
		super(layoutManager);
	}

}
