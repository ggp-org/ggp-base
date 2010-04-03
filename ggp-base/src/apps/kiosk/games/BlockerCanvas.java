package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

import apps.kiosk.GridGameCanvas;

public class BlockerCanvas extends GridGameCanvas {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Blocker"; }
    protected String getGameKIF() { return "blocker"; }
    protected int getGridHeight() { return 6; }
    protected int getGridWidth() { return 6; }
   
    private int selectedRow = -1;
    private int selectedColumn = -1;    
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        if(xCell == 0 || xCell == 5 || yCell == 0 || yCell == 5)
            return;
        
        if(gameStateHasLegalMove("( mark " + xCell + " " + yCell + " )")) {
            selectedRow = yCell;
            selectedColumn = xCell; 
            submitWorkingMove(stringToMove("( mark " + selectedColumn + " " + selectedRow + " )"));
        }
    }

    protected void renderCell(int xCell, int yCell, Graphics g) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        boolean isBlue = (yCell == 0) || (yCell == 5);
        boolean isBlack = ((xCell == 0) || (xCell == 5)) && !isBlue;
        
        if(gameStateHasFact("( cell " + xCell + " " + yCell + " blk )") || isBlue) {
            g.setColor(Color.BLUE);
            g.fillRect(1, 1, width-2, height-2);
            drawBubbles(g, xCell, yCell);
        } else if(gameStateHasFact("( cell " + xCell + " " + yCell + " crosser )") || isBlack) {
            g.setColor(Color.GRAY);
            g.fillRect(1, 1, width-2, height-2);
        } else {
            ;
        }

        if(selectedColumn == xCell && selectedRow == yCell) {
            g.setColor(Color.GREEN);
            g.drawRect(3, 3, width-6, height-6);
        }
    }
    
    private void drawBubbles(Graphics g, int x, int y) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        Random r = new Random(x + 11*y);
        for(int i = 0; i < 4; i++) {
            int xB = (int)(r.nextDouble() * width);
            int yB = (int)(r.nextDouble() * height);
            int rB = (int)(r.nextDouble() * Math.min(width, height)/5.0);
            g.setColor(Color.CYAN);
            g.drawOval(xB-rB, yB-rB, rB*2, rB*2);
        }
    }
    
    public void clearMoveSelection() {        
        submitWorkingMove(null);        
        selectedColumn = -1;    
        selectedRow = -1;
        
        repaint();
    }    
}