package org.ggp.base.apps.kiosk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;


public class GameGUI extends JPanel implements Subject, Observer, ActionListener {
    public static final long serialVersionUID = 0x1;

    private GameCanvas theCanvas;
    private Move workingMove;

    private JLabel workingMoveLabel;
    private JButton submitMoveButton;
    private JButton clearSelectionButton;
    
    private boolean gameOver = false;
    
    private boolean moveBeingSubmitted = false;
    private boolean stillMetagaming = true;
    
    public GameGUI(GameCanvas theCanvas) {
        super(new BorderLayout());        
        
        this.theCanvas = theCanvas;

        JLabel theTitleLabel = new JLabel(theCanvas.getGameName());
        theTitleLabel.setFont(new Font(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()[0], Font.BOLD, 36));        
        
        JPanel northPanel = new JPanel(new FlowLayout());
        northPanel.add(theTitleLabel);
                
        submitMoveButton = new JButton("Submit Move");
        submitMoveButton.addActionListener(this);
        
        clearSelectionButton = new JButton("Clear Selection");
        clearSelectionButton.addActionListener(this);
        
        workingMoveLabel = new JLabel();
        
        JPanel southCenterPanel = new JPanel(new FlowLayout());
        JPanel southEastPanel = new JPanel(new FlowLayout());
        JPanel southPanel = new JPanel(new BorderLayout());
        southEastPanel.add(new JLabel("Time Remaining     "));
        southEastPanel.add(clearSelectionButton);
        southEastPanel.add(submitMoveButton);
        southPanel.add("West", workingMoveLabel);
        southPanel.add("Center", southCenterPanel);
        southPanel.add("East", southEastPanel);
        
        add("North", northPanel);
        add("Center", theCanvas);
        add("South", southPanel);
        
        northPanel.setBackground(theCanvas.getBackground());
        southPanel.setBackground(theCanvas.getBackground());
        southEastPanel.setBackground(theCanvas.getBackground());
        southCenterPanel.setBackground(theCanvas.getBackground());

        theCanvas.addObserver(this);
        updateControls();
    }
    
    public void beginPlay() {
    	stillMetagaming = false;
    	updateControls();
    }
    
    public void updateGameState(MachineState gameState) {
    	moveBeingSubmitted = false;
        theCanvas.updateGameState(gameState);
        updateControls();
    }
    
    public void setRole(Role r) {
        theCanvas.setRole(r);
    }

    @Override
    public void observe(Event event) {
        if(event instanceof MoveSelectedEvent) {
            workingMove = ((MoveSelectedEvent)event).getMove();            
            if(((MoveSelectedEvent)event).isFinal()) {
            	moveBeingSubmitted = true;
            	updateControls();            	
            	notifyObservers(new MoveSelectedEvent(workingMove));            	
            }               
            updateControls();
        }
    }
    
    private void updateControls() {
        submitMoveButton.setEnabled(!gameOver && !moveBeingSubmitted && !stillMetagaming);
        clearSelectionButton.setEnabled(!gameOver && !moveBeingSubmitted && !stillMetagaming);
        theCanvas.setEnabled(!gameOver && !moveBeingSubmitted && !stillMetagaming);
        
        if(gameOver) return;        
        if(workingMove == null) {
            workingMoveLabel.setText("  Working Move: <none>");
            submitMoveButton.setEnabled(false);
            clearSelectionButton.setEnabled(false);
        } else {
            workingMoveLabel.setText("  Working Move: " + workingMove);
        }
    }
    
    public void showFinalMessage(String theMessage) {
        workingMoveLabel.setText(theMessage);
        workingMoveLabel.setForeground(Color.RED);
        gameOver = true;
        updateControls();
        
        validate();
        repaint();        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(gameOver) return;
        
        if(e.getSource() == clearSelectionButton) {
            theCanvas.clearMoveSelection();            
        } else if(e.getSource() == submitMoveButton) {
            if(workingMove != null) {
                moveBeingSubmitted = true;
                updateControls();
                notifyObservers(new MoveSelectedEvent(workingMove));
            }
        }
    }
    
    // Subject boilerplate
    private Set<Observer> theObservers = new HashSet<Observer>();

    @Override    
    public void addObserver(Observer observer) {
        theObservers.add(observer);        
    }

    @Override
    public void notifyObservers(Event event) {
        for(Observer theObserver : theObservers)
            theObserver.observe(event);        
    }
}