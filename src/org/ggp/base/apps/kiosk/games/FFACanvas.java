package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_FancyGrid;


public class FFACanvas extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Free-For-All"; }
    protected String getGameKey() { return "2pffa"; }
    protected int getGridHeight() { return 7; }
    protected int getGridWidth() { return 7; }

    protected final boolean useGridVisualization() { return false; }
    protected final boolean coordinatesStartAtOne() { return true; }
    
    protected final void renderCellBackground(Graphics g, int xCell, int yCell) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        CommonGraphics.drawCellBorder(g);
        
        // Clear out the edges
        if(xCell == 1 || xCell == 7 || yCell == 1 || yCell == 7) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);            
        }
    }    
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( move " + xCell + " " + yCell + " (.*) \\)");
        
        if(theMoves.size() == 0)
            theMoves.add("noop");
        
        return theMoves;
    }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        Set<String> theFacts = gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
        
        if (xCell == 1 && yCell == 1) {
            theFacts.addAll(gameStateHasFactsMatching("\\( capture red (.*) \\)"));
        } else if(xCell == 7 && yCell == 1) {
            theFacts.addAll(gameStateHasFactsMatching("\\( capture blue (.*) \\)"));
        }

        return theFacts;
    }

    @Override
    protected void renderCellContent(Graphics g, Set<String> theFacts) {
        if(theFacts.size() == 0) return;
        String theFact = theFacts.iterator().next();
        
        String[] cellFacts = theFact.split(" ");
        if(cellFacts.length == 5) {
            String cellType = cellFacts[2];
            int score = 10*Integer.parseInt(cellFacts[3]);
            
            Color myColor = null;
            if(cellType.startsWith("red")) myColor = Color.red;
            if(cellType.startsWith("blue")) myColor = Color.blue;            
            if(myColor == null) {
                System.err.println("Got weird piece: " + cellType);
                return;
            }

            g.setColor(myColor);
            CommonGraphics.fillWithString(g, "" + score, 1.5);
        } else {
            String cellType = cellFacts[4];
            if(!cellType.equals("b")) {
                Color myColor = null;
                if(cellType.startsWith("red")) myColor = Color.red;
                if(cellType.startsWith("blue")) myColor = Color.blue;            
                if(myColor == null) {
                    System.err.println("Got weird piece: " + cellType);
                    return;
                }
    
                int width = g.getClipBounds().width;
                int height = g.getClipBounds().height;              
                
                g.setColor(myColor);
                g.fillOval(2, 2, width-4, height-4);
                CommonGraphics.drawChessPiece(g, "wn");
            }
        }
    }    
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;        

        String[] moveParts = theMove.split(" ");
        
        if(moveParts.length == 7) {
            int xTarget = Integer.parseInt(moveParts[4]);
            int yTarget = Integer.parseInt(moveParts[5]);
            if(xTarget == xCell && yCell == yTarget) {
                g.setColor(Color.GREEN);
                g.drawRect(3, 3, width-6, height-6);
                g.drawRect(4, 4, width-8, height-8);
            }
        }
    }
}