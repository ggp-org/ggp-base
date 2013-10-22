package org.ggp.base.apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import org.ggp.base.apps.kiosk.templates.CommonGraphics;
import org.ggp.base.apps.kiosk.templates.GameCanvas_SimpleGrid;


public class TTTxNineCanvas extends GameCanvas_SimpleGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Nine Board Tic Tac Toe"; }
    protected String getGameKey() { return "nineBoardTicTacToe"; }
    protected int getGridHeight() { return 9; }
    protected int getGridWidth() { return 9; }

    protected boolean coordinatesStartAtOne() { return false; }
    
    private int xSelectedBoard = 0;
    private int ySelectedBoard = 0;
    private int xSelectedSpot = 0;
    private int ySelectedSpot = 0;
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        int xBoard = 1 + (xCell / 3);
        int yBoard = 1 + (yCell / 3);
        int xSpot = 1 + (xCell % 3);
        int ySpot = 1 + (yCell % 3);

        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( play " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " (.*) \\)");                
        if(theMoves.size() == 1) {
            xSelectedSpot = xSpot;
            ySelectedSpot = ySpot;
            xSelectedBoard = xBoard;
            ySelectedBoard = yBoard;
            submitWorkingMove(stringToMove(theMoves.iterator().next()));
        }
    }

    @Override
    protected void renderCell(Graphics g, int xCell, int yCell) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;

        int xBoard = 1 + (xCell / 3);
        int yBoard = 1 + (yCell / 3);
        int xSpot = 1 + (xCell % 3);
        int ySpot = 1 + (yCell % 3);        
        
        g.setColor(Color.BLACK);
        g.drawRect(7, 7, width-14, height-14);
        
        if(gameStateHasFact("( currentBoard " + xBoard + " " + yBoard + " )") ||
           gameStateHasFactsMatching("\\( currentBoard (.*) (.*) \\)").size() == 0) {
            g.setColor(Color.BLUE);
        }
        if(xSpot == 1) g.fillRect(0, 0, 5, height);
        if(xSpot == 3) g.fillRect(width-5, 0, 5, height);
        if(ySpot == 1) g.fillRect(0, 0, width, 5);
        if(ySpot == 3) g.fillRect(0, height-5, width, 5);
        
        if(gameStateHasFact("( mark " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " x )")) {
            g.setColor(Color.GRAY);
            g.fillRect(8, 8, width-15, height-15);                        
            g.setColor(Color.BLACK);
            CommonGraphics.fillWithString(g, "X", 1.2);
        } else if(gameStateHasFact("( mark " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " o )")) {
            g.setColor(Color.GRAY);
            g.fillRect(8, 8, width-15, height-15);            
            g.setColor(Color.WHITE);
            CommonGraphics.fillWithString(g, "O", 1.2);
        } else {
            ;
        }
        
        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( play " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " (.*) \\)");                
        if(theMoves.size() == 1) {        
            g.setColor(Color.GRAY);
            for(int i = 8; i < 10; i++)
                g.drawRect(i, i, width-2*i, height-2*i);
        }
        
        if(xSelectedSpot == xSpot && ySelectedSpot == ySpot &&
           xSelectedBoard == xBoard && ySelectedBoard == yBoard) {
            g.setColor(Color.GREEN);
            g.fillRect(10, 10, width-19, height-19);            
        }        
    }
    
    @Override    
    public void clearMoveSelection() {        
        submitWorkingMove(null);
        xSelectedSpot = 0;
        ySelectedSpot = 0;
        
        repaint();
    }
}

/*
public class TTTxNineCanvas extends GameCanvas_SimpleGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Nine Board Tic Tac Toe"; }
    protected String getGameKey() { return "tictactoex9"; }
    protected int getGridHeight() { return 9; }
    protected int getGridWidth() { return 9; }

    protected boolean coordinatesStartAtOne() { return false; }
    
    private int xSelectedBoard = 0;
    private int ySelectedBoard = 0;
    private int xSelectedSpot = 0;
    private int ySelectedSpot = 0;
    protected void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
        int xBoard = 1 + (xCell / 3);
        int yBoard = 1 + (yCell / 3);
        int xSpot = 1 + (xCell % 3);
        int ySpot = 1 + (yCell % 3);
        
        String theMove = "( mark " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " )";
        if(gameStateHasLegalMove(theMove)) {
            xSelectedSpot = xSpot;
            ySelectedSpot = ySpot;
            xSelectedBoard = xBoard;
            ySelectedBoard = yBoard;
            submitWorkingMove(stringToMove(theMove));
        }
    }

    @Override
    protected void renderCell(Graphics g, int xCell, int yCell) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;

        int xBoard = 1 + (xCell / 3);
        int yBoard = 1 + (yCell / 3);
        int xSpot = 1 + (xCell % 3);
        int ySpot = 1 + (yCell % 3);        
        
        g.setColor(Color.BLACK);
        g.drawRect(7, 7, width-14, height-14);
        
        if(gameStateHasFact("( boardtoplay " + xBoard + " " + yBoard + " )") ||
           gameStateHasFact("( boardtoplay any any )")) {
            g.setColor(Color.BLUE);
        }
        if(xSpot == 1) g.fillRect(0, 0, 5, height);
        if(xSpot == 3) g.fillRect(width-5, 0, 5, height);
        if(ySpot == 1) g.fillRect(0, 0, width, 5);
        if(ySpot == 3) g.fillRect(0, height-5, width, 5);
        
        if(gameStateHasFact("( cell " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " x )")) {
            g.setColor(Color.GRAY);
            g.fillRect(8, 8, width-15, height-15);                        
            g.setColor(Color.BLACK);
            CommonGraphics.fillWithString(g, "X", 1.2);
        } else if(gameStateHasFact("( cell " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " o )")) {
            g.setColor(Color.GRAY);
            g.fillRect(8, 8, width-15, height-15);            
            g.setColor(Color.WHITE);
            CommonGraphics.fillWithString(g, "O", 1.2);
        } else {
            ;
        }
        
        String theMove = "( mark " + xBoard + " " + yBoard + " " + xSpot + " " + ySpot + " )";
        if(gameStateHasLegalMove(theMove)) {
            g.setColor(Color.GRAY);
            for(int i = 8; i < 10; i++)
                g.drawRect(i, i, width-2*i, height-2*i);
        }
        
        if(xSelectedSpot == xSpot && ySelectedSpot == ySpot &&
           xSelectedBoard == xBoard && ySelectedBoard == yBoard) {
            g.setColor(Color.GREEN);
            g.fillRect(10, 10, width-19, height-19);            
        }        
    }
    
    @Override    
    public void clearMoveSelection() {        
        submitWorkingMove(null);
        xSelectedSpot = 0;
        ySelectedSpot = 0;
        
        repaint();
    }
}*/