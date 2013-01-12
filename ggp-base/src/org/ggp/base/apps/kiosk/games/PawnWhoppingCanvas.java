package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_Chessboard;


public class PawnWhoppingCanvas extends GameCanvas_Chessboard {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Pawn Whopping"; }
    protected String getGameKey() { return "pawnWhopping"; }

    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( move " + xCell + " " + yCell + " (.*) \\)");
        theMoves.addAll(gameStateHasLegalMovesMatching("\\( capture " + xCell + " " + yCell + " (.*) \\)"));
        return theMoves;
    }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        return gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
    }

    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");
        String cellType = cellFacts[4];
        if(cellType.equals("x")) {
            CommonGraphics.drawChessPiece(g, "wp");
            
            g.setColor(Color.black);
            CommonGraphics.fillWithString(g, "x", 5.0);
        } else if(cellType.equals("o")) {
            CommonGraphics.drawChessPiece(g, "bp");
            
            g.setColor(getBackground());
            CommonGraphics.fillWithString(g, "o", 5.0);
        }
    }    
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;        
        
        String[] moveParts = theMove.split(" ");
        int xTarget = Integer.parseInt(moveParts[4]);
        int yTarget = Integer.parseInt(moveParts[5]);
        if(xCell == xTarget && yCell == yTarget) {
            g.setColor(new Color(0, 0, 255, 192));                
            g.drawRect(3, 3, width-6, height-6);
            CommonGraphics.fillWithString(g, "X", 3);
        }
    }
}