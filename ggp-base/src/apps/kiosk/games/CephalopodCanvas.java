package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;
import java.util.Set;

import apps.kiosk.GridGameCanvas;

public class CephalopodCanvas extends GridGameCanvas {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Cephalopod"; }
    protected String getGameKIF() { return "CephalopodMicro"; }
    protected int getGridHeight() { return 3; }
    protected int getGridWidth() { return 3; }
    
    private int selectedRow = -1;
    private int selectedColumn = -1; 
    private String currentSelectedMove;
    private Iterator<String> possibleSelectedMoves = null;
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        xCell++;
        yCell++;
        
        if(selectedRow != yCell || selectedColumn != xCell || !possibleSelectedMoves.hasNext()) {
            Set<String> theMoves = gameStateHasLegalMovesMatching("\\( play " + xCell + " " + yCell + " (.*) \\)");
            if(theMoves.size() == 0)
                return;
            possibleSelectedMoves = theMoves.iterator();            
        }
        
        selectedRow = yCell;
        selectedColumn = xCell;
        
        currentSelectedMove = possibleSelectedMoves.next();        
        submitWorkingMove(stringToMove(currentSelectedMove));
    }

    protected void renderCell(int xCell, int yCell, Graphics g) {
        xCell++;
        yCell++;        
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        Set<String> theFacts = gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
        if(theFacts.size() > 0) {
            if(theFacts.size() > 1) {
                System.err.println("More than one fact for a cell? Something is weird!");
            }
            
            String[] cellFacts = theFacts.iterator().next().split(" ");
        
            int cellValue = Integer.parseInt(cellFacts[4]);
            String cellPlayer = cellFacts[5];
    
            if (cellPlayer.equals("red")) {
                g.setColor(Color.RED);
            } else if (cellPlayer.equals("black")) {
                g.setColor(Color.BLACK);
            }
            
            fillWithString(g, "" + cellValue, 1.2);  
        }
        
        if(selectedColumn == xCell && selectedRow == yCell) {
            g.setColor(Color.GREEN);
            g.drawRect(3, 3, width-6, height-6);
            
            // Within this cell, render the graphics specific to the
            // current move that we've selected (since each cell can
            // have multiple moves associated with it).
            String[] moveParts = currentSelectedMove.split(" ");
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
    
    public void clearMoveSelection() {        
        submitWorkingMove(null);
        
        possibleSelectedMoves = null;
        currentSelectedMove = "";
        selectedColumn = -1;    
        selectedRow = -1;
        
        repaint();
    }    
}