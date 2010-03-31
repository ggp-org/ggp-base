package apps.kiosk;

import java.awt.Graphics;

public abstract class GridGameCanvas extends GameCanvas {
    public static final long serialVersionUID = 0x1;

    protected abstract int getGridWidth();
    protected abstract int getGridHeight();
    protected abstract void renderCell(int x, int y, Graphics g);
    protected abstract void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin);
    
    private Graphics mostRecentG;
    protected void paintGame(Graphics g) {
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
                renderCell(x, y, cellGraphics);
            }
        }        
    }

    protected void handleClickEvent(int x, int y) {
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
        
        handleClickOnCell(xCell, yCell, xWithin, yWithin);
    }    
}