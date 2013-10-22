package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_FancyGrid;


public class CephalopodCanvas extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Cephalopod"; }
    protected String getGameKey() { return "cephalopodMicro"; }
    protected int getGridHeight() { return 3; }
    protected int getGridWidth() { return 3; }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        return gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
    }
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        return gameStateHasLegalMovesMatching("\\( play " + xCell + " " + yCell + " (.*) \\)");
    }
    
    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");
        
        int cellValue = Integer.parseInt(cellFacts[4]);
        String cellPlayer = cellFacts[5];

        if (cellPlayer.equals("red")) {
            g.setColor(Color.RED);
        } else if (cellPlayer.equals("black")) {
            g.setColor(Color.BLACK);
        }
        
        CommonGraphics.fillWithString(g, "" + cellValue, 1.2);
    }
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        if(isSelectedCell(xCell, yCell)) {
            String[] moveParts = theMove.split(" ");
            int captureMask = Integer.parseInt(moveParts[5]);
            renderCaptureMask(g, captureMask);
        }
    }
    
    private void renderCaptureMask(Graphics g, int c) {
        boolean leftBit = (c == 3 || c == 5 || c == 7 || c == 9 || c == 11 || c == 13 || c == 15);
        boolean rightBit = (c >= 9);
        boolean topBit = (c == 3 || c == 6 || c == 7 || c == 10 || c == 11 || c == 14 || c == 15);
        boolean bottomBit = (c == 5 || c == 6 || c == 7 || c >= 12);
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.GREEN);
        if(leftBit) g.drawRect(width/10, 3*height/10, width/20, 4*height/10);
        if(rightBit) g.drawRect(17*width/20, 3*height/10, width/20, 4*height/10);
        if(topBit) g.drawRect(3*width/10, height/10, 4*width/10, height/20);
        if(bottomBit) g.drawRect(3*width/10, 17*height/20, 4*width/10, height/20);
    }    
}