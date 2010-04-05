package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.util.Iterator;
import java.util.Set;

import apps.kiosk.GridGameCanvas;

public class CheckersCanvas extends GridGameCanvas {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Checkers"; }
    protected String getGameKIF() { return "checkers"; }
    protected int getGridHeight() { return 8; }
    protected int getGridWidth() { return 8; }
    
    private int selectedRow = -1;
    private int selectedColumn = -1; 
    private String currentSelectedMove;
    private Iterator<String> possibleSelectedMoves = null;
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        String xLetter = "" + ((char) ('a' + xCell));
        
        xCell++;
        yCell++;
        
        if(selectedRow != yCell || selectedColumn != xCell || !possibleSelectedMoves.hasNext()) {
            Set<String> theMoves = gameStateHasLegalMovesMatching("\\( move .. " + xLetter + " " + yCell + " (.*) \\)");
            theMoves.addAll(gameStateHasLegalMovesMatching("\\( doublejump .. " + xLetter + " " + yCell + " (.*) \\)"));
            theMoves.addAll(gameStateHasLegalMovesMatching("\\( triplejump .. " + xLetter + " " + yCell + " (.*) \\)"));
            if(theMoves.size() == 0)
                return;
            possibleSelectedMoves = theMoves.iterator();            
        }
        
        selectedRow = yCell;
        selectedColumn = xCell;
        
        currentSelectedMove = possibleSelectedMoves.next();        
        submitWorkingMove(stringToMove(currentSelectedMove));
    }

    private final Image theCrownImage = getImage("crown.png");
    protected void renderCell(int xCell, int yCell, Graphics g) {
        String xLetter = "" + ((char) ('a' + xCell));
        
        xCell++;
        yCell++;
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        // Alternating colors for the board
        if( (xCell + yCell) % 2 == 0) {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, width, height);
            return;
        }
        
        g.setColor(Color.BLACK);
        g.drawRect(1, 1, width-2, height-2);
        
        Set<String> theFacts = gameStateHasFactsMatching("\\( cell " + xLetter + " " + yCell + " (.*) \\)");
        if(theFacts.size() > 0) {
            if(theFacts.size() > 1) {
                System.err.println("More than one fact for a cell? Something is weird!");
            }
            
            String[] cellFacts = theFacts.iterator().next().split(" ");
            String cellType = cellFacts[4];
            if(!cellType.equals("b")) {
                Color theColor = ((cellType.charAt(0) == 'b') ? Color.BLACK : Color.RED);
                boolean isKing = (cellType.charAt(1) == 'k');
                
                g.setColor(Color.DARK_GRAY);
                g.fillOval(4, 4, width-8, height-8);
                g.setColor(theColor);
                g.fillOval(6, 6, width-12, height-12);
                if(isKing) {
                    g.setColor(Color.YELLOW);
                    g.drawImage(theCrownImage, width/5, 2*height/7, 3*width/5, 3*height/7, null);
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
            String xTarget = moveParts[5];
            int yTarget = Integer.parseInt(moveParts[6]);
            if(xLetter.equals(xTarget) && yCell == yTarget) {
                g.setColor(Color.BLUE);
                g.drawRect(3, 3, width-6, height-6);
                fillWithString(g, "X", 3);
            }
            if(moveParts.length > 8) {
                xTarget = moveParts[7];
                yTarget = Integer.parseInt(moveParts[8]);
                if(xLetter.equals(xTarget) && yCell == yTarget) {
                    g.setColor(Color.BLUE);
                    g.drawRect(3, 3, width-6, height-6);
                    fillWithString(g, "Y", 3);
                }
            }
            if(moveParts.length > 10) {
                xTarget = moveParts[9];
                yTarget = Integer.parseInt(moveParts[10]);
                if(xLetter.equals(xTarget) && yCell == yTarget) {
                    g.setColor(Color.BLUE);
                    g.drawRect(3, 3, width-6, height-6);
                    fillWithString(g, "Z", 3);
                }
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