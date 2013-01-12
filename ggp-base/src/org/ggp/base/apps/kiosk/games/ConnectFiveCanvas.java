package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_Chessboard;


public class ConnectFiveCanvas extends GameCanvas_Chessboard {
    public static final long serialVersionUID = 0x1;
    
    public String getGameName() { return "Connect Five"; }
    protected String getGameKey() { return "connect5"; }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        String xLetter = coordinateToLetter(xCell);
        String yLetter = coordinateToLetter(yCell);        
        return gameStateHasFactsMatching("\\( cell " + xLetter + " " + yLetter + " (.*) \\)");
    }
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        String xLetter = coordinateToLetter(xCell);
        String yLetter = coordinateToLetter(yCell);        
        return gameStateHasLegalMovesMatching("\\( mark " + xLetter + " " + yLetter + " \\)");
    }
    
    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");
        if(cellFacts[4].equals("b")) return;
        
        g.setColor(Color.BLACK);
        CommonGraphics.fillWithString(g, cellFacts[4].toUpperCase(), 1.2);
    }   
}