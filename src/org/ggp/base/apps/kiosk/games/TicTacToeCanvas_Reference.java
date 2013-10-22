package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import org.ggp.base.apps.kiosk.GameCanvas;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;


/**
 * A reference implementation of Tic-Tac-Toe based purely on GameCanvas,
 * not using any of the standard templates. Comes to just under 150 lines.
 * 
 * @author Sam Schreiber
 */
public class TicTacToeCanvas_Reference extends GameCanvas {
    public static final long serialVersionUID = 0x1;
    
    public String getGameName() {
        return "Tic-Tac-Toe (Old Version)";
    }
    
    protected String getGameKey() {
        return "ticTacToe";
    }    

    private Graphics mostRecentG;
    protected void paintGame(Graphics g) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        mostRecentG = g;
        
        g.setColor(this.getBackground());
        g.fillRect(0, 0, width, height);
        
        g.setColor(Color.BLACK);
        g.fillRect(width/3-2, 0, 4, height);
        g.fillRect(2*width/3-2, 0, 4, height);
        g.fillRect(0, height/3-2, width, 4);
        g.fillRect(0, 2*height/3-2, width, 4);
        
        if(gameState == null)
            return;
        
        for(GdlSentence stateFact : gameState.getContents()) {
            if(stateFact.getBody().size() > 1)
                continue;
            
            GdlSentence theRealSentence = stateFact.getBody().get(0).toSentence();
            if(theRealSentence.getName().toString().equals("cell")) {
                int xCell = Integer.parseInt(theRealSentence.getBody().get(0).toString());
                int yCell = Integer.parseInt(theRealSentence.getBody().get(1).toString());
                char mark = theRealSentence.getBody().get(2).toString().charAt(0);
                
                int xSpot = (xCell-1)*width/3 + 2;
                int ySpot = (yCell-1)*height/3 + 2;
                                
                g.setColor(Color.BLACK);
                final int BORDER_SIZE = 35;                
                if(mark == 'b') {
                    ;
                } else if(mark == 'x') {
                    g.drawLine(xSpot+BORDER_SIZE, ySpot+BORDER_SIZE, xSpot + width/3 - BORDER_SIZE, ySpot + height/3 - BORDER_SIZE);
                    g.drawLine(xSpot+BORDER_SIZE, ySpot + height/3 - BORDER_SIZE, xSpot + width/3 - BORDER_SIZE, ySpot+BORDER_SIZE);
                } else if(mark == 'o') {
                    g.drawOval(xSpot+BORDER_SIZE, ySpot+BORDER_SIZE, width/3 - 2*BORDER_SIZE, height/3 - 2*BORDER_SIZE);
                }
            }                        
        }
        
        try {
            List<Move> legalMoves = stateMachine.getLegalMoves(gameState, myRole);
            
            for(Move legalMove : legalMoves) {
                if(legalMove.getContents() instanceof GdlConstant)
                    continue;
                
                GdlFunction moveContents = (GdlFunction) legalMove.getContents();
				int xCell = Integer.parseInt(moveContents.getBody().get(0).toString());
                int yCell = Integer.parseInt(moveContents.getBody().get(1).toString());
                
                int xSpot = (xCell-1)*width/3 + 2;
                int ySpot = (yCell-1)*height/3 + 2;
                
                final int BORDER_SIZE = 60;
                g.setColor(Color.GREEN);
                if(myRole.toString().equals("xplayer")) {
                    g.drawLine(xSpot+BORDER_SIZE, ySpot+BORDER_SIZE, xSpot + width/3 - BORDER_SIZE, ySpot + height/3 - BORDER_SIZE);
                    g.drawLine(xSpot+BORDER_SIZE, ySpot + height/3 - BORDER_SIZE, xSpot + width/3 - BORDER_SIZE, ySpot+BORDER_SIZE);
                } else {
                    g.drawOval(xSpot+BORDER_SIZE, ySpot+BORDER_SIZE, width/3 - 2*BORDER_SIZE, height/3 - 2*BORDER_SIZE);
                }
                
                if(xSelectedCell == xCell && ySelectedCell == yCell) {
                    g.drawRect(xSpot+BORDER_SIZE/2, ySpot+BORDER_SIZE/2, width/3-BORDER_SIZE, height/3-BORDER_SIZE);
                }
            }
        } catch (MoveDefinitionException e) {
            ;
        }
    }

    protected void handleDragEvent(int dx, int dy) {
        ;
    }

    private int xSelectedCell, ySelectedCell;
    protected void handleClickEvent(int x, int y) {
        int width = mostRecentG.getClipBounds().width;
        int height = mostRecentG.getClipBounds().height;        
        
        try {
            List<Move> legalMoves = stateMachine.getLegalMoves(gameState, myRole);
            
            int xCell = 1+3*x/width;
            int yCell = 1+3*y/height;
            
            String moveString = "( mark " + xCell + " " + yCell + " )";
            for(Move legalMove : legalMoves) {
                if(legalMove.toString().equals(moveString)) {                    
                    submitWorkingMove(legalMove);
                    
                    xSelectedCell = xCell;
                    ySelectedCell = yCell;
                    
                    repaint();
                }
            }
        } catch(Exception e) {
            ;
        }
    }
    
    public void clearMoveSelection() {
        xSelectedCell = ySelectedCell = 0;
        submitWorkingMove(null);
        
        repaint();
    }
}