package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_SimpleGrid;


public class GoldenRectangleCanvas extends GameCanvas_SimpleGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Golden Rectangle"; }
    protected String getGameKey() { return "golden_rectangle"; }
    protected int getGridHeight() { return 8; }
    protected int getGridWidth() { return 7; }

    private int selectedColumn = 0;    
    
    @Override
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {        
        yCell = 8 - yCell;
        
        for (int y = 0; y <= 7; y++) {
            if(gameStateHasLegalMove("( mark " + xCell + " " + y + " )")) {
                selectedColumn = xCell;
                submitWorkingMove(stringToMove("( mark " + xCell + " " + y + " )"));
            }
        }
    }

    @Override
    protected void renderCell(Graphics g, int xCell, int yCell) {
        yCell = 8 - yCell;
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        if(gameStateHasFact("( cell " + xCell + " " + yCell + " r )")) {
            g.setColor(Color.RED);
            CommonGraphics.drawDisc(g);
        } else if(gameStateHasFact("( cell " + xCell + " " + yCell + " y )")) {
            g.setColor(Color.YELLOW);
            CommonGraphics.drawDisc(g);
        } else {
            ;
        }

        if(selectedColumn == xCell) {
            g.setColor(Color.GREEN);
            g.drawRect(3, 3, width-6, height-6);
        }
    }
    
    @Override
    public void clearMoveSelection() {        
        submitWorkingMove(null);
        selectedColumn = 0;
        
        repaint();
    }
}