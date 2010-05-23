package apps.kiosk;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JPanel;

import player.gamer.statemachine.StateMachineGamer;
import server.event.ServerCompletedMatchEvent;
import server.event.ServerNewGameStateEvent;
import util.observer.Event;
import util.observer.Observer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class KioskGamer extends StateMachineGamer implements Observer {
    private BlockingQueue<Move> theQueue = new ArrayBlockingQueue<Move>(25);
    
    private GameGUI theGUI;
    private JPanel theGUIPanel;
    public KioskGamer(JPanel theGUIPanel) {        
        this.theGUIPanel = theGUIPanel;
        theGUIPanel.setLayout(new BorderLayout());
    }
    
    private GameCanvas theCanvas = null;
    public void setCanvas(GameCanvas theCanvas) {
        this.theCanvas = theCanvas;
    }
    
    @Override
    public void stateMachineMetaGame(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {
        if(theCanvas == null)
            System.err.println("KioskGamer did not receive a canvas.");
        theCanvas.setStateMachine(getStateMachine());
        
        theGUI = new GameGUI(theCanvas);
        theGUI.setRole(getRole());
        theGUI.updateGameState(getStateMachine().getInitialState());
        theGUI.addObserver(this);
        
        theGUIPanel.removeAll();
        theGUIPanel.add("Center", theGUI);
        theGUIPanel.repaint();
        
        theGUIPanel.setVisible(false);
        theGUIPanel.setVisible(true);
    }

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {
    	theGUI.beginPlay();
        theQueue.clear();
        theGUI.updateGameState(getCurrentState());
        try {
            return theQueue.take();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public StateMachine getInitialStateMachine() {
        return new ProverStateMachine();
    }

    @Override
    public String getName() {
        return "GraphicalHumanGamer";
    }
    
    private MachineState stateFromServer;

    @Override    
    public void observe(Event event) {
        if(event instanceof MoveSelectedEvent) {
            Move theMove = ((MoveSelectedEvent)event).getMove();
            if(theQueue.size() < 2) {
                theQueue.add(theMove);
            }
        } else if(event instanceof ServerNewGameStateEvent) {
            stateFromServer = ((ServerNewGameStateEvent)event).getState();
        } else if(event instanceof ServerCompletedMatchEvent) {
            theGUI.updateGameState(stateFromServer);
            
            List<Role> theRoles = getStateMachine().getRoles();
            List<Integer> theGoals = ((ServerCompletedMatchEvent)event).getGoals();
            
            StringBuilder finalMessage = new StringBuilder();
            finalMessage.append("Goals: ");
            for(int i = 0; i < theRoles.size(); i++) {
                finalMessage.append(theRoles.get(i));
                finalMessage.append(" = ");
                finalMessage.append(theGoals.get(i));
                if(i < theRoles.size()-1) {
                    finalMessage.append(", ");
                }
            }
            
            theGUI.showFinalMessage(finalMessage.toString());
        }
    }
    
}