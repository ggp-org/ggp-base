package org.ggp.base.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.presence.PlayerPresence;
import org.ggp.base.util.presence.PlayerPresenceManager;
import org.ggp.base.util.presence.PlayerPresenceManager.InvalidHostportException;

public class PlayerSelector {
	private PlayerPresenceManager thePresenceManager;
	
    public PlayerSelector() {
    	thePresenceManager = new PlayerPresenceManager();
    }
    
    class PlayerPresenceLabel extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;
		private int maxLabelLength;

		public PlayerPresenceLabel(int maxLabelLength) {
    		setOpaque(true);
    		setHorizontalAlignment(CENTER);
    		setVerticalAlignment(CENTER);
    		this.maxLabelLength = maxLabelLength;
    	}
    	
    	public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
			setHorizontalAlignment(JLabel.LEFT);
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			
			PlayerPresence presence = thePresenceManager.getPresence(value.toString());
			String status = presence.getStatus();
			if (status != null) status = status.toLowerCase();
			
			int iconSize = 20;			
			
			BufferedImage img = new BufferedImage( iconSize, iconSize, BufferedImage.TYPE_INT_RGB );
			Graphics g = img.getGraphics();			 
			g.setColor( getBackground() );
			g.fillRect( 0, 0, iconSize, iconSize );
			if (status == null) {
				g.setColor(Color.GRAY);
			} else if (status.equals("available")) {
				g.setColor(Color.GREEN);
			} else if (status.equals("busy")) {
				g.setColor(Color.ORANGE);
			} else if (status.equals("error")) {
				g.setColor(Color.BLACK);
			} else {
				g.setColor(Color.MAGENTA);
			}
			
			g.fillOval( 3, 3, iconSize-6, iconSize-6 );
			
			String textLabel = presence.getHost() + ":" + presence.getPort();
			if (presence.getName() != null) {
				textLabel = presence.getName() + " (" + textLabel + ")";
			}
			if (textLabel.length() > maxLabelLength) {
				textLabel = textLabel.substring(0, maxLabelLength-3) + "...";
			}
			
			setIcon(new ImageIcon(img));
			setText(textLabel);
			setFont(list.getFont());		
			return this;
    	}
    }
    
    class PlayerSelectorBox extends JComboBox implements Observer {
		private static final long serialVersionUID = 1L;

		public PlayerSelectorBox() {
    		thePresenceManager.addObserver(this);
    		setRenderer(new PlayerPresenceLabel(20));
    		addAllPlayerItems();
    	}
		
		private void addAllPlayerItems() {
        	for (String name : thePresenceManager.getSortedPlayerNames()) {
        		addItem(name);
        	}			
		}

		@Override
		public void observe(Event event) {
			if (event instanceof PlayerPresenceManager.PlayerPresenceChanged) {
				repaint();
				revalidate();
			} else if (event instanceof PlayerPresenceManager.PlayerPresenceAdded ||
					    event instanceof PlayerPresenceManager.PlayerPresenceRemoved) {				
				Object currentlySelected = getSelectedItem();
				removeAllItems();
				addAllPlayerItems();
				setSelectedItem(currentlySelected);
				repaint();
				revalidate();
			}
		}
    }
    
    class PlayerSelectorList extends JList implements Observer {
		private static final long serialVersionUID = 1L;

		public PlayerSelectorList() {
    		thePresenceManager.addObserver(this);
    		setCellRenderer(new PlayerPresenceLabel(40));
    		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    		setAllPlayerItems();
    	}
		
		private void setAllPlayerItems() {
			setListData(thePresenceManager.getSortedPlayerNames().toArray());
		}

		@Override
		public void observe(Event event) {
			if (event instanceof PlayerPresenceManager.PlayerPresenceChanged) {
				repaint();
				revalidate();				
			} else if (event instanceof PlayerPresenceManager.PlayerPresenceAdded ||
					    event instanceof PlayerPresenceManager.PlayerPresenceRemoved) {
				Object currentlySelected = getSelectedValue();
				setAllPlayerItems();
				setSelectedValue(currentlySelected, true);
				repaint();
				revalidate();
			}
		}    	
    }
    
    public void addPlayer(String hostport) throws InvalidHostportException {
    	thePresenceManager.addPlayer(hostport);
    }
    
    public void removePlayer(String hostport) {
    	thePresenceManager.removePlayer(hostport);
    }    
    
    public PlayerPresence getPlayerPresence(String name) {
    	return thePresenceManager.getPresence(name);
    }
    
    public JComboBox getPlayerSelectorBox() {
    	return new PlayerSelectorBox();
    }
    
    public JList getPlayerSelectorList() {
    	return new PlayerSelectorList();
    }    
}