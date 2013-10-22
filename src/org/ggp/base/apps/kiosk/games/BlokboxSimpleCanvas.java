package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_SimpleGrid;
import org.ggp.base.util.statemachine.MachineState;


public class BlokboxSimpleCanvas extends GameCanvas_SimpleGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Blokbox Simple"; }
    protected String getGameKey() { return "blokbox_simple"; }
    
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        if(selectedPiece == -1) return new HashSet<String>();
        return gameStateHasLegalMovesMatching("\\( place " + selectedPiece + " " + xCell + " " + yCell + " \\)");
    }

    // ========================================================================
    protected int getGridHeight() { return 20; }
    protected int getGridWidth() { return 20; }
    
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        Set<String> theFacts = gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
        if(xCell >= 15 || yCell >= 15) {
            int nPiece = pieceGrid[xCell-1][yCell-1];
            theFacts.addAll(gameStateHasFactsMatching("\\( owns " + myRole + " " + nPiece + " \\)")); 
        }
        return theFacts;
    }
        
    private int[][] pieceGrid = new int[][] {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 8, 8, 8}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21,21, 0, 8, 0}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0,18, 0,18}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0,18,18,18}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0, 6, 0, 0, 0}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  6, 6, 0, 5, 5}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0, 0,20, 0, 0}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0,20,20,20, 0}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0,20, 0, 7}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10,10, 0, 0, 7}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0,10, 0, 0, 7}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 1, 1, 0}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 1, 1, 0}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14,14, 0, 0,15}, 
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 0, 2, 0,15}, 
            { 0, 0, 0,12,12,12, 0,13,13, 0,11,11,11, 0,14,14, 0, 2, 0,15}, 
            { 0, 0, 0,12, 0, 0,17, 0,13,13, 0,11, 0, 4, 0, 0, 2, 2, 0,15}, 
            { 0, 0, 0,12, 0, 0,17,17, 0,13, 0,11, 0, 0,19,19, 0, 0, 0,15}, 
            { 0, 0, 0, 0,16,16, 0,17, 0, 0, 0, 0, 0,19,19, 0, 3, 0, 0, 0}, 
            { 0, 0, 0,16,16,16, 0,17, 0, 9, 9, 9, 9, 0,19, 0, 3, 3, 3, 3}
    };
    
    protected void renderCellBackground(Graphics g, int xCell, int yCell) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        if ((xCell == 15 && yCell <= 15) || (yCell == 15 && xCell <= 15)) {                        
            if (xCell == 15) width /= 2;
            if (yCell == 15) height /= 2;
            width++;
            height++;
            
            int xStart = 0, yStart = 0;
            if (xCell == 15 || yCell == 0) xStart++;
            if (yCell == 15 || xCell == 0) yStart++;
            if (xCell == 15 && yCell == 15)
                { xStart--; yStart--; width++; height++; }
            
            g.setColor(Color.black);
            g.fillRect(xStart, yStart, width, height);
            g.fillRect(xStart, yStart, width, height);
            return;
        }
        
        if (xCell <= 14 && yCell <= 14) {
            CommonGraphics.drawCellBorder(g);
            return;
        }

        g.setColor(Color.black);
        if (xCell == 16 && yCell == 2) CommonGraphics.fillWithString(g, "B", 2.0);
        if (xCell == 17 && yCell == 2) CommonGraphics.fillWithString(g, "L", 2.0);
        if (xCell == 18 && yCell == 2) CommonGraphics.fillWithString(g, "O", 2.0);
        if (xCell == 19 && yCell == 2) CommonGraphics.fillWithString(g, "K", 2.0);
        if (xCell == 20 && yCell == 2) CommonGraphics.fillWithString(g, "S", 2.0);
    }
    
    protected void renderCellContent(Graphics g, String theFact) {        
        String[] cellFacts = theFact.split(" ");
        if(cellFacts[1].equals("owns")) {
            if (myRole.toString().contains("orange"))
                 g.setColor(Color.orange);
            else g.setColor(Color.magenta);
            
            int width = g.getClipBounds().width;
            int height = g.getClipBounds().height;            
            g.fillRect(0, 0, width, height);
            g.setColor(Color.black);
            g.drawRect(0, 0, width, height);
        } else {
            String cellPlayer = cellFacts[4];
            if (cellPlayer.equals("orange")) {
                g.setColor(Color.ORANGE);
            } else if (cellPlayer.equals("purple")) {
                g.setColor(Color.MAGENTA);
            }
            int width = g.getClipBounds().width;
            int height = g.getClipBounds().height;            
            g.fillRect(0, 0, width, height);
            g.setColor(Color.black);
            g.drawRect(0, 0, width, height);
        }
    }
    
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        if (selectedPiece == pieceGrid[xCell-1][yCell-1]) {
            CommonGraphics.drawSelectionBox(g);
            return;
        }
        
        if (selectedRow == yCell && selectedColumn == xCell) {
            CommonGraphics.drawSelectionBox(g);
            return;
        }
    }
    
    
    //////////////////////////////////////////////////////////////////////
    // We need to re-implement part of the FancyGrid architecture, because
    // we need certain parts but need to change other parts (specifically,
    // we want FancyGrid behavior within the 14x14 game grid, but not when
    // the player is interacting with the pieces sidebar).

    protected void renderCellContent(Graphics g, Set<String> theFacts){
        if(theFacts.size() > 0) {
            if(theFacts.size() > 1) {
                System.err.println("More than one fact for a cell? Unexpected!");
            }

            String theFact = theFacts.iterator().next();
            renderCellContent(g, theFact);
        }        
    }
    
    protected int selectedPiece = -1;
    
    private int selectedRow = -1;
    private int selectedColumn = -1; 
    private String currentSelectedMove;
    private Iterator<String> possibleSelectedMoves = null;
    protected final void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        if(xCell > 20 || yCell > 20) return;
        
        if(xCell > 15 || yCell > 15) {
            int nPiece = pieceGrid[xCell-1][yCell-1];
            selectedPiece = -1;
            if (nPiece > 0 && getCachedFactsAboutCell(xCell, yCell).size() > 0)
                selectedPiece = nPiece;
            selectedRow = -1;
            selectedColumn = -1;
            currentSelectedMove = "";
            possibleSelectedMoves = null;
            submitWorkingMove(null);
            factsCache.clear();
            return;
        }
        if(selectedPiece == -1) return;
        
        if(selectedRow != yCell || selectedColumn != xCell || !possibleSelectedMoves.hasNext()) {
            SortedSet<String> theMoves = new TreeSet<String>(getLegalMovesForCell(xCell, yCell));
            if(theMoves.size() == 0)
                return;
            possibleSelectedMoves = theMoves.iterator();            
        }
        selectedRow = yCell;
        selectedColumn = xCell;
        
        currentSelectedMove = possibleSelectedMoves.next();        
        submitWorkingMove(stringToMove(currentSelectedMove));
    }

    // Cache all of the facts about cells that we compute, since they should not
    // change unless the game state changes.
    private Map<Integer, Set<String>> factsCache = new HashMap<Integer, Set<String>>();
    protected Set<String> getCachedFactsAboutCell(int xCell, int yCell) {
        int cellHash = xCell*getGridHeight()*2 + yCell;
        Set<String> cachedFacts = factsCache.get(cellHash);
        if(cachedFacts != null)
            return cachedFacts;
        
        Set<String> realFacts = getFactsAboutCell(xCell, yCell);
        factsCache.put(cellHash, realFacts);
        return realFacts;
    }
    
    // When the game state changes, clear our cache of known facts.
    public void updateGameState(MachineState gameState) {
        factsCache.clear();
        super.updateGameState(gameState);
    }
    
    protected final void renderCell(Graphics g, int xCell, int yCell) {
        renderCellBackground(g, xCell, yCell);
        renderCellContent(g, getCachedFactsAboutCell(xCell, yCell));
        renderMoveSelectionForCell(g, xCell, yCell, currentSelectedMove);
    }

    public final void clearMoveSelection() {        
        submitWorkingMove(null);
        
        selectedPiece = -1;
        possibleSelectedMoves = null;
        currentSelectedMove = "";
        selectedColumn = -1;    
        selectedRow = -1;
        
        repaint();
    }        
}