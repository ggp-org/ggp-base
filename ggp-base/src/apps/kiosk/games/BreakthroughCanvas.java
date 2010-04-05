package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import apps.kiosk.GridGameCanvas;

public class BreakthroughCanvas extends GridGameCanvas {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Breakthrough"; }
    protected String getGameKIF() { return "breakthrough"; }
    protected int getGridHeight() { return 8; }
    protected int getGridWidth() { return 8; }
    
    private int selectedRow = -1;
    private int selectedColumn = -1; 
    private String currentSelectedMove;
    private Iterator<String> possibleSelectedMoves = null;
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        xCell++;
        yCell++;
        
        if(selectedRow != yCell || selectedColumn != xCell || !possibleSelectedMoves.hasNext()) {
            SortedSet<String> theMoves = new TreeSet<String>(gameStateHasLegalMovesMatching("\\( move " + xCell + " " + yCell + " (.*) \\)"));
            if(theMoves.size() == 0)
                return;
            possibleSelectedMoves = theMoves.iterator();            
        }
        
        selectedRow = yCell;
        selectedColumn = xCell;
        
        currentSelectedMove = possibleSelectedMoves.next();        
        submitWorkingMove(stringToMove(currentSelectedMove));
    }

    private final Image wPawnImage = getImage("wpawn.gif");
    private final Image bPawnImage = getImage("bpawn.gif");
    protected void renderCell(int xCell, int yCell, Graphics g) {
        xCell++;
        yCell++;
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        // Alternating colors for the board
        if( (xCell + yCell) % 2 == 0) {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, width, height);
        }
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        Set<String> theFacts = gameStateHasFactsMatching("\\( cellholds " + xCell + " " + yCell + " (.*) \\)");
        if(theFacts.size() > 0) {
            if(theFacts.size() > 1) {
                System.err.println("More than one fact for a cell? Something is weird!");
            }
            
            String[] cellFacts = theFacts.iterator().next().split(" ");
            String cellType = cellFacts[4];
            if(!cellType.equals("b")) {                
                /*
                // Simple rendering without using images
                Color theColor = ((cellType.charAt(0) == 'w') ? Color.WHITE : Color.BLACK); 
                Color oppColor = ((cellType.charAt(0) == 'b') ? Color.WHITE : Color.BLACK);
                g.setColor(Color.DARK_GRAY);
                g.fillOval(4, 4, width-8, height-8);
                g.setColor(theColor);
                g.fillOval(6, 6, width-12, height-12);       
                g.setColor(oppColor);
                fillWithString(g, "p", 2);
                */

                if(cellType.charAt(0) == 'w') {
                    g.drawImage(wPawnImage, 0, 0, width, height, null);
                } else {
                    g.drawImage(bPawnImage, 0, 0, width, height, null);
                }
            }
        }
        
        if(selectedColumn == xCell && selectedRow == yCell) {
            g.setColor(Color.GREEN);
            g.drawRect(3, 3, width-6, height-6);
        }
                
        // Render move-specific graphics
        if(!currentSelectedMove.isEmpty()) {
            String[] moveParts = currentSelectedMove.split(" ");
            int xTarget = Integer.parseInt(moveParts[4]);
            int yTarget = Integer.parseInt(moveParts[5]);
            if(xCell == xTarget && yCell == yTarget) {
                g.setColor(new Color(0, 0, 255, 192));                
                g.drawRect(3, 3, width-6, height-6);
                fillWithString(g, "X", 3);
            }
        }
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