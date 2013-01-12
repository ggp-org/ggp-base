package org.ggp.base.apps.kiosk.templates;

import java.awt.Graphics;

import org.ggp.base.apps.kiosk.GameCanvas;


/**
 * GameCanvas_SimpleGrid builds a very simple grid on top of the
 * standard Kiosk game canvas. This makes the following simplifying
 * assumptions about the structure of the game:
 * 
 *      1) Each grid cell in the game can be rendered independently.
 *      2) Click events should be handled on a per-cell basis.
 *      
 * Thus, it overrides paintGame() and handleClickEvent() and instead
 * provides four new abstract functions:
 * 
 *      getGridWidth          Return the width of the grid.
 *      getGridHeight         Return the height of the grid.
 *      renderCell            Draw an individual grid cell.
 *      handleClickOnCell     Handle a click on a grid cell.
 *      
 * You can also optionally override function "coordinatesStartAtOne"
 * to control whether or not the coordinates passed to the above functions
 * start at zero or one. This is a convenience feature, since many game
 * descriptions are written assuming that the game grid has coordinates
 * which start at one. By default, coordinates *do* start at one.
 * 
 * @author Sam Schreiber
 */
public abstract class GameCanvas_SimpleGrid extends GameCanvas {
    public static final long serialVersionUID = 0x1;
    
    protected abstract int getGridWidth();
    protected abstract int getGridHeight();
    protected abstract void renderCell(Graphics g, int x, int y);
    protected abstract void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin);
    
    protected boolean coordinatesStartAtOne() { return true; }
    
    private Graphics mostRecentG;
    protected final void paintGame(Graphics g) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        mostRecentG = g;
        
        g.setColor(this.getBackground());
        g.fillRect(0, 0, width, height);

        if(gameState == null)
            return;
        
        int nGridWidth = getGridWidth();
        int nGridHeight = getGridHeight();
        
        int nCellWidth = width / nGridWidth;
        int nCellHeight = height / nGridHeight;
        
        for(int x = 0; x < nGridWidth; x++) {
            for(int y = 0; y < nGridHeight; y++) {
                Graphics cellGraphics = g.create(x*nCellWidth, y*nCellHeight, nCellWidth, nCellHeight);
                if(coordinatesStartAtOne()) {
                    renderCell(cellGraphics, x+1, y+1);
                } else {
                    renderCell(cellGraphics, x, y);
                }
            }
        }        
    }

    protected final void handleClickEvent(int x, int y) {
        int width = mostRecentG.getClipBounds().width;
        int height = mostRecentG.getClipBounds().height;        

        int nGridWidth = getGridWidth();
        int nGridHeight = getGridHeight();
        
        int nCellWidth = width / nGridWidth;
        int nCellHeight = height / nGridHeight;
        
        int xCell = x / nCellWidth;
        int yCell = y / nCellHeight;
        
        int xWithin = x % nCellWidth;
        int yWithin = y % nCellHeight;
        
        if(coordinatesStartAtOne()) {
            handleClickOnCell(xCell+1, yCell+1, xWithin, yWithin);
        } else {
            handleClickOnCell(xCell, yCell, xWithin, yWithin);
        }
    }        
}