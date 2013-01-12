package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_FancyGrid;


public class NumberTicTacToeCanvas extends GameCanvas_FancyGrid {
    public static final long serialVersionUID = 0x1;
    
    public String getGameName() { return "Tic-Tac-Toe (Numeric)"; }
    protected String getGameKey() { return "numbertictactoe"; }
    protected int getGridHeight() { return 3; }
    protected int getGridWidth() { return 3; }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        return gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
    }
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        return gameStateHasLegalMovesMatching("\\( mark " + xCell + " " + yCell + " (.*) \\)");
    }
    
    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");
        
        if(!cellFacts[4].equals("b")) {
            g.setColor(Color.BLACK);
            CommonGraphics.fillWithString(g, cellFacts[4], 1.2);
        }
    }   
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        if(!isSelectedCell(xCell, yCell)) return;
        
        g.setColor(Color.GREEN);
        String[] moveFacts = theMove.split(" ");        
        CommonGraphics.fillWithString(g, "" + moveFacts[4], 1.2);
    }
}