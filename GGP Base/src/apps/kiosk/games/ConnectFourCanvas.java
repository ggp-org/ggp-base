package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;

import apps.kiosk.GridGameCanvas;

public class ConnectFourCanvas extends GridGameCanvas {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Connect Four"; }
    protected String getGameKIF() { return "connectfour"; }
    protected int getGridHeight() { return 6; }
    protected int getGridWidth() { return 8; }

    private int selectedColumn = 0;    
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        yCell = 5 - yCell;
        xCell++;
        yCell++;
        
        if(gameStateHasLegalMove("( drop " + xCell + " )")) {
            selectedColumn = xCell;
            submitWorkingMove(stringToMove("( drop " + xCell + " )"));
        }
    }

    protected void renderCell(int xCell, int yCell, Graphics g) {
        yCell = 5 - yCell;
        xCell++;
        yCell++;
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        if(gameStateHasFact("( cell " + xCell + " " + yCell + " red )")) {
            g.setColor(Color.RED);
            g.fillOval(5, 5, width-10, height-10);
        } else if(gameStateHasFact("( cell " + xCell + " " + yCell + " black )")) {
            g.setColor(Color.BLACK);
            g.fillOval(5, 5, width-10, height-10);
        } else {
            ;
        }

        if(selectedColumn == xCell) {
            g.setColor(Color.GREEN);
            g.drawRect(3, 3, width-6, height-6);
        }
    }
    
    public void clearMoveSelection() {        
        submitWorkingMove(null);
        selectedColumn = 0;
        
        repaint();
    }    
}