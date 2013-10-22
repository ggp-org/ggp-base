package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_FancyGrid;


public class BlockerCanvas extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Blocker"; }
    protected String getGameKey() { return "blocker"; }
    protected int getGridHeight() { return 6; }
    protected int getGridWidth() { return 6; }

    protected boolean coordinatesStartAtOne() { return false; }    

    @Override
    protected void renderCellBackground(Graphics g, int xCell, int yCell) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;                    
        
        boolean isBlue = (yCell == 0) || (yCell == 5);
        boolean isBlack = ((xCell == 0) || (xCell == 5)) && !isBlue;        
        
        if(isBlue) {
            CommonGraphics.drawBubbles(g, xCell*11+yCell);
        } else if(isBlack) {            
            g.setColor(Color.GRAY);
            g.fillRect(1, 1, width-2, height-2);
        }
    }
    
    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;

        String[] theFacts = theFact.split(" ");
        String theProperty = theFacts[4];
        if(theProperty.equals("blk")) {
            CommonGraphics.drawBubbles(g, theFact.hashCode());
        } else if(theProperty.equals("crosser")) {
            g.setColor(Color.GRAY);
            g.fillRect(1, 1, width-2, height-2);
        }
    }

    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        return gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
    }
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        return gameStateHasLegalMovesMatching("\\( mark " + xCell + " " + yCell + " \\)");
    }
}