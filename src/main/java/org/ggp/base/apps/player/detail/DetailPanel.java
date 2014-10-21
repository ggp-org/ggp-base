package org.ggp.base.apps.player.detail;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import org.ggp.base.util.observer.Observer;

/**
 * Gamers can have optional "detail panels" which display their status while
 * they're playing matches. This can be as simple as a table listing the number
 * of moves they're considering and which one they've selected, or it can show
 * complex pieces of debug information like the number of match simulations done
 * per second or the expected payoff from taking various moves.
 */
@SuppressWarnings("serial")
public abstract class DetailPanel extends JPanel implements Observer
{
	public DetailPanel(LayoutManager layoutManager)
	{
		super(layoutManager);
	}
}