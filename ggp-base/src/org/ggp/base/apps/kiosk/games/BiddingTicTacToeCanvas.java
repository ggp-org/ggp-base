package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashSet;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_FancyGrid;


public class BiddingTicTacToeCanvas extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Tic-Tac-Toe (Bidding)"; }
    protected String getGameKey() { return "biddingTicTacToe"; }
    protected int getGridHeight() { return 5; }
    protected int getGridWidth() { return 7; }
    
    protected boolean coordinatesStartAtOne() { return false; }
    protected boolean useGridVisualization() { return false; }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        if(onGrid(xCell, yCell)) {
            return gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
        } else {
            int nScore = onScoreboard(xCell, yCell);
            if(nScore == -2) {
                return gameStateHasFactsMatching("\\( tiebreaker " + myRole + " \\)");
            } else if (nScore != -1) {
                return gameStateHasFactsMatching("\\( coins " + myRole + " " + nScore + " \\)");
            }
        }
        return new HashSet<String>();
    }

    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        if(onGrid(xCell, yCell)) {
            return gameStateHasLegalMovesMatching("\\( mark " + xCell + " " + yCell + " \\)");
        } else {
            int nScore = onScoreboard(xCell, yCell);
            if(nScore >= 0) {
                return gameStateHasLegalMovesMatching("\\( bid " + nScore + " (.*) \\)");
            }          
        }
        return new HashSet<String>();
    }

    @Override
    protected void renderCellBackground(Graphics g, int xCell, int yCell) {
        g.setColor(Color.GRAY);
        
        if(onGrid(xCell, yCell)) {
            CommonGraphics.drawCellBorder(g);
        } else {        
            int nScore = onScoreboard(xCell, yCell);
            if(nScore >= 0) {
                CommonGraphics.fillWithString(g, "" + nScore, 1.2);
            } else if(nScore == -2) {
                CommonGraphics.fillWithString(g, "T", 1.2);
            }
        }
    }
    
    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");

        g.setColor(Color.BLACK);
        if(cellFacts[1].equals("cell")) {
            String cellPlayer = cellFacts[4];
            if(cellPlayer.equals("b")) return;
            CommonGraphics.fillWithString(g, cellPlayer, 1.2);
        } else if(cellFacts[1].equals("coins")) {
            CommonGraphics.fillWithString(g, cellFacts[3], 1.2);
        } else if(cellFacts[1].equals("tiebreaker")) {
            CommonGraphics.fillWithString(g, "T", 1.2);
        }
    }
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        String[] moveParts = theMove.split(" ");
        
        if(isSelectedCell(xCell, yCell))
            CommonGraphics.drawSelectionBox(g);
        
        if(moveParts[1].equals("bid") && moveParts[3].equals("with_tiebreaker")) {
            if(xCell == 6 && yCell == 4) {
                CommonGraphics.drawSelectionBox(g);
            }
        }
    }
    
    private boolean onGrid(int xCell, int yCell) {
        return (xCell <= 3 && xCell >= 1 && yCell <= 3 && yCell >= 1);
    }
    
    private int onScoreboard(int xCell, int yCell) {
        if(xCell < 5) return -1;
        if(xCell == 5 && yCell == 4) return -1;
        if(xCell == 6 && yCell == 3) return -1;
        if(xCell == 6 && yCell == 4) return -2;
        return yCell + (xCell == 6 ? 4 : 0);
    }
}