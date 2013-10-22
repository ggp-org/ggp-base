package org.ggp.base.apps.kiosk.games;

import java.awt.Graphics;
import java.awt.Image;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_FancyGrid;


public class PentagoCanvas extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Pentago"; }
    protected String getGameKey() { return "pentago"; }
    protected int getGridHeight() { return 6; }
    protected int getGridWidth() { return 6; }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        int nQuad = getQuadrant(xCell, yCell);
        xCell = ((xCell > 3) ? (xCell - 3) : xCell);
        yCell = ((yCell > 3) ? (yCell - 3) : yCell);        
        return gameStateHasFactsMatching("\\( cellHolds " + nQuad + " " + xCell + " " + yCell + " (.*) \\)");
    }

    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        int nQuad = getQuadrant(xCell, yCell);
        xCell = ((xCell > 3) ? (xCell - 3) : xCell);
        yCell = ((yCell > 3) ? (yCell - 3) : yCell);
        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( place " + nQuad + " " + xCell + " " + yCell + " \\)");
        theMoves.addAll(gameStateHasLegalMovesMatching("\\( rotate " + nQuad + " (.*) \\)"));
        return theMoves;
    }

    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");

        String cellPlayer = cellFacts[5];
        CommonGraphics.drawCheckersPiece(g, cellPlayer.equals("red") ? "wp" : "bp");
    }

    private Image left_arrow = CommonGraphics.getImage("pentago", "BlueCircleArrowCCW.png");
    private Image right_arrow = CommonGraphics.getImage("pentago", "BlueCircleArrowCW.png");
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        String[] moveParts = theMove.split(" ");
        if(moveParts[1].equals("rotate")) {
            int nQuad = Integer.parseInt(moveParts[2]);
            if(getQuadrant(xCell, yCell) == nQuad) {
                CommonGraphics.drawSelectionBox(g);
                
                if(!(xCell == 2 || xCell == 5)) return;
                if(!(yCell == 2 || yCell == 5)) return;
                
                int width = g.getClipBounds().width;
                int height = g.getClipBounds().height;
                if(moveParts[3].equals("clockwise")) {
                    g.drawImage(left_arrow, 15, 15, width-30, height-30, null);
                } else {
                    g.drawImage(right_arrow, 15, 15, width-30, height-30, null);                    
                }
            }
        }
    }
    
    private int getQuadrant(int xCell, int yCell) {
        if(xCell > 3) {
            if(yCell > 3) return 1;
            else return 4;
        } else {
            if (yCell > 3) return 2;
            else return 3;
        }
    }
}