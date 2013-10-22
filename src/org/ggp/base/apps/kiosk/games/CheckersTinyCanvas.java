package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_Chessboard;


public class CheckersTinyCanvas extends GameCanvas_Chessboard {
    private static final long serialVersionUID = 1L;    
    
    public String getGameName() { return "Checkers (Tiny)"; }
    protected String getGameKey() { return "checkersTiny"; }
    
    protected int getGridHeight() { return 8; }
    protected int getGridWidth() { return 8; }        

    @Override
    protected void renderCellForeground(Graphics g, int xCell, int yCell) {
        xCell-=2;
        
        if(xCell <= 0 || xCell >= 5) {
            int width = g.getClipBounds().width;
            int height = g.getClipBounds().height;            
            
            g.setColor(Color.DARK_GRAY);            
            g.fillRect(0, 0, width, height);
        }
    }
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        xCell-=2;
        
        String xLetter = coordinateToLetter(xCell);        
        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( move .. " + xLetter + " " + yCell + " (.*) \\)");
        theMoves.addAll(gameStateHasLegalMovesMatching("\\( doublejump .. " + xLetter + " " + yCell + " (.*) \\)"));
        theMoves.addAll(gameStateHasLegalMovesMatching("\\( triplejump .. " + xLetter + " " + yCell + " (.*) \\)"));
        return theMoves;
    }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        xCell-=2;
        
        String xLetter = coordinateToLetter(xCell);
        return gameStateHasFactsMatching("\\( cell " + xLetter + " " + yCell + " (.*) \\)");
    }

    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");
        String cellType = cellFacts[4];
        if(!cellType.equals("b")) {
            CommonGraphics.drawCheckersPiece(g, cellType);
        }
    }    
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        xCell-=2;
        
        String xLetter = coordinateToLetter(xCell);

        String[] moveParts = theMove.split(" ");
        String xTarget = moveParts[5];
        int yTarget = Integer.parseInt(moveParts[6]);
        if(xLetter.equals(xTarget) && yCell == yTarget) {
            g.setColor(Color.BLUE);
            g.drawRect(3, 3, width-6, height-6);
            CommonGraphics.fillWithString(g, "X", 3);
        }
        if(moveParts.length > 8) {
            xTarget = moveParts[7];
            yTarget = Integer.parseInt(moveParts[8]);
            if(xLetter.equals(xTarget) && yCell == yTarget) {
                g.setColor(Color.BLUE);
                g.drawRect(3, 3, width-6, height-6);
                CommonGraphics.fillWithString(g, "Y", 3);
            }
        }
        if(moveParts.length > 10) {
            xTarget = moveParts[9];
            yTarget = Integer.parseInt(moveParts[10]);
            if(xLetter.equals(xTarget) && yCell == yTarget) {
                g.setColor(Color.BLUE);
                g.drawRect(3, 3, width-6, height-6);
                CommonGraphics.fillWithString(g, "Z", 3);
            }
        }
    }
}