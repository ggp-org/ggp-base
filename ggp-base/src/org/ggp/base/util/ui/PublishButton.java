package org.ggp.base.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.ggp.base.server.GameServer;


@SuppressWarnings("serial")
public class PublishButton extends JButton implements ActionListener {
    private GameServer theServer;
    
    public PublishButton(String theName) {
        super(theName);
        this.addActionListener(this);
        this.setEnabled(false);
    }
    
    public void setServer(GameServer theServer) {
        this.theServer = theServer;
        this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this) {
            if (theServer != null) {
                if (!theServer.getMatch().getGame().getRepositoryURL().contains("127.0.0.1")) {
                    String theMatchKey = theServer.startPublishingToSpectatorServer("http://matches.ggp.org/");
                    if (theMatchKey != null) {
                        String theURL = "http://www.ggp.org/view/all/matches/" + theMatchKey + "/";
                        System.out.println("Publishing to: " + theURL);
                        int nChoice = JOptionPane.showConfirmDialog(this,
                                "Publishing successfully. Would you like to open the spectator view in a browser?",
                                "Publishing Match Online",
                                JOptionPane.YES_NO_OPTION);         
                        if (nChoice == JOptionPane.YES_OPTION) {                        
                            try {
                                java.awt.Desktop.getDesktop().browse(java.net.URI.create(theURL));
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Unknown problem when publishing match.",
                                "Publishing Match Online",
                                JOptionPane.ERROR_MESSAGE);                        
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Could not publish a game that is only stored locally.",
                        "Publishing Match Online",
                        JOptionPane.ERROR_MESSAGE);
                }
                setEnabled(false);
            }
        }
    }
}