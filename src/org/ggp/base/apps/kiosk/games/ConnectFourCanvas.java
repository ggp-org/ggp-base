package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_SimpleGrid;


public class ConnectFourCanvas extends GameCanvas_SimpleGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Connect Four"; }
    protected String getGameKey() { return "connectFour"; }
    protected int getGridHeight() { return 6; }
    protected int getGridWidth() { return 8; }

    private int selectedColumn = 0;    
    
    @Override
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        yCell = 7 - yCell;
        
        if(gameStateHasLegalMove("( drop " + xCell + " )")) {
            selectedColumn = xCell;
            submitWorkingMove(stringToMove("( drop " + xCell + " )"));
        }
    }

    @Override
    protected void renderCell(Graphics g, int xCell, int yCell) {
        yCell = 7 - yCell;
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        if(gameStateHasFact("( cell " + xCell + " " + yCell + " red )")) {
            g.setColor(Color.RED);
            CommonGraphics.drawCheckersPiece(g, "wp");
        } else if(gameStateHasFact("( cell " + xCell + " " + yCell + " black )")) {
            g.setColor(Color.BLACK);
            CommonGraphics.drawCheckersPiece(g, "bp");
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