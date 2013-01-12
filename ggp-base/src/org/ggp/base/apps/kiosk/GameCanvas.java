package org.ggp.base.apps.kiosk;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;


public abstract class GameCanvas extends JPanel implements Subject {
    public static final long serialVersionUID = 0x1;
    
    // Store the information about the current state of the game
    protected StateMachine stateMachine; 
    protected MachineState gameState;    
    protected Role myRole;
    
    // Cache the location of the last click 
    private int lastClickX;
    private int lastClickY;

    // Border constant
    private static final int BORDER_SIZE = 10;
    
    public GameCanvas() {
        super();
        setFocusable(true);
        
        // Fiddle with Mouse Settings       
        addMouseListener( new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                
                // Update the click point cache
                lastClickX = e.getX();
                lastClickY = e.getY();
                handleClickEventWrapper(lastClickX, lastClickY);
                
                repaint();
            }
            
            public void mouseReleased(MouseEvent e){
                lastClickX = -1;
                lastClickY = -1;                
            }
        });     
        
        addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if(lastClickX == -1)
                    return;

                // compute delta from last point
                int dx = e.getX() - lastClickX;
                int dy = e.getY() - lastClickY;
                handleDragEventWrapper(dx, dy);
                
                // Update the click point cache
                lastClickX = e.getX();
                lastClickY = e.getY();
            }            
        });        
    }
    
    public void setStateMachine(StateMachine s) {
    	stateMachine = s;
    }
    
    public void setRole(Role r) {
        myRole = r;
    }
    
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        
        g.setColor(Color.BLACK);
        g.drawRect(4, 4, getWidth() - 8, getHeight() - 8);
        g.drawRect(6, 6, getWidth() - 12, getHeight() - 12);
        
        if(!isEnabled()) {
        	g.setColor(Color.red);
        	g.drawRect(5, 5, getWidth() - 10, getHeight() - 10);
        }
        
        Graphics newG = g.create(BORDER_SIZE, BORDER_SIZE, getWidth() - 2*BORDER_SIZE, getHeight() - 2*BORDER_SIZE);
        if(gameState != null) {
            paintGame(newG);
        } else {
            paintGameDefault(newG, "Waiting for game state...");
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

    protected void submitWorkingMove(Move theMove) {        
        notifyObservers(new MoveSelectedEvent(theMove));
    }
    
    protected void submitFinalMove(Move theMove) {        
        notifyObservers(new MoveSelectedEvent(theMove, true));
    }
    
    public void updateGameState(MachineState gameState) {
        this.gameState = gameState;
        clearMoveSelection();
        
        try {
            List<Move> legalMoves = stateMachine.getLegalMoves(gameState, myRole);
            if(legalMoves.size() > 1) {
                submitWorkingMove(null);
            } else {
                //submitWorkingMove(legalMoves.get(0));
            	submitFinalMove(legalMoves.get(0));
            }            
        } catch (MoveDefinitionException e) {
            submitWorkingMove(null);
        }        
    }
    
    private void paintGameDefault(Graphics g, String message) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        g.setColor(Color.BLACK);
        g.drawString(message, width/2 - message.length()*2, height/2);
    }    

    /* ---------- Convenience methods ---------- */
    protected boolean gameStateHasFact(String fact) {
        String trueFact = "( true " + fact + " )";
        for(GdlSentence aFact : gameState.getContents()) {
            if(aFact.toString().equals(trueFact))
                return true;
        }
        return false;
    }

    protected Set<String> gameStateHasFactsMatching(String ptrn) {
        Pattern pattern = Pattern.compile("\\( true " + ptrn + " \\)");

        Set<String> theMatches = new HashSet<String>();
        for(GdlSentence theFact : gameState.getContents()) {
            Matcher matcher = pattern.matcher(theFact.toString());
            if (matcher.find()) {
                String match = matcher.group();
                match = match.substring(7, match.length() - 2);
                theMatches.add(match);
            }
        }
        return theMatches;
    }
    
    protected boolean gameStateHasLegalMove(String move) {
        try {
            List<Move> legalMoves = stateMachine.getLegalMoves(gameState, myRole);
            for(Move aMove : legalMoves) {
                if(aMove.toString().equals(move))
                    return true;
            }
            return false;             
        } catch(Exception e) {
            return false;
        }       
    }
    
    protected Set<String> gameStateHasLegalMovesMatching(String ptrn) {
        Pattern pattern = Pattern.compile(ptrn);

        Set<String> theMatches = new HashSet<String>();
        try {
            List<Move> legalMoves = stateMachine.getLegalMoves(gameState, myRole);
            for(Move theMove : legalMoves) {
                Matcher matcher = pattern.matcher(theMove.toString());
                if (matcher.find()) {
                    String match = matcher.group();
                    theMatches.add(match);
                }
            }
            return theMatches;
        } catch (MoveDefinitionException e) {
            // This can happen in games where the terminal state
            // has no legal moves defined for a player.
            //e.printStackTrace();
            return new HashSet<String>();
        }        
    }
    

    protected Move stringToMove(String move) {
        try {
            return stateMachine.getMoveFromTerm(GdlFactory.createTerm(move));
        } catch(Exception e) {
            return null;
        }
    }        

    /* ---------- Enabling wrappers ------------ */
    
    private void handleDragEventWrapper(int dx, int dy) {
    	if(!isEnabled()) return;
    	handleDragEvent(dx, dy);
    }
    
    private void handleClickEventWrapper(int x, int y) {
    	if(!isEnabled()) return;
    	handleClickEvent(x, y);
    } 
    
    /* ---------- For overriding! -------------- */

    public abstract String getGameName();
    
    protected abstract String getGameKey();

    protected void paintGame(Graphics g) {        
        paintGameDefault(g, "paintGame not implemented");
    }

    protected void handleDragEvent(int dx, int dy) {
        ;
    }

    protected void handleClickEvent(int x, int y) {
        ;
    }
    
    public abstract void clearMoveSelection();
}