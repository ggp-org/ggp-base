package org.ggp.base.util.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class CloseableTabs {
	private final static int CLOSE_ICON_SIZE = 8;
	
	public static void addClosableTab(JTabbedPane tabPane, JComponent tabContent, String tabName, AbstractAction closeAction) {
		JPanel tabTopPanel = new JPanel(new GridBagLayout());
		tabTopPanel.setOpaque(false);
				
		JButton btnClose = new JButton();		
		BufferedImage img = new BufferedImage(CLOSE_ICON_SIZE, CLOSE_ICON_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = img.getGraphics();			 
		g.setColor(Color.BLACK);
		g.drawLine(0, CLOSE_ICON_SIZE-1, CLOSE_ICON_SIZE-1, 0);
		g.drawLine(0, 0, CLOSE_ICON_SIZE-1, CLOSE_ICON_SIZE-1);
		btnClose.setIcon(new ImageIcon(img));
		btnClose.addActionListener(closeAction);		

		tabTopPanel.add(new JLabel(tabName), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		tabTopPanel.add(btnClose, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 4, 0, 0), 0, 0));

		tabPane.addTab(tabName, tabContent);
		tabPane.setTabComponentAt(tabPane.getTabCount()-1, tabTopPanel);
	}
}