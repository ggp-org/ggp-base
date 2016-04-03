package org.ggp.base.apps.kiosk.templates;

import java.awt.Color;
import java.awt.Graphics;

/**
 * GameCanvas_Chessboard is a fixed grid template designed for visualizing
 * games that are played on a chess board. It establishes the basic graphical
 * conventions for this class of games: an 8x8 board with alternating colors.
 * It also defines the coordinates-to-letters mapping that appears in several
 * chess board games, such as chess and checkers.
 *
 * @author Sam Schreiber
 */
public abstract class GameCanvas_Chessboard extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    @Override
    protected int getGridHeight() { return 8; }
    @Override
    protected int getGridWidth() { return 8; }

    @Override
    protected final boolean useGridVisualization() { return true; }
    @Override
    protected final boolean coordinatesStartAtOne() { return true; }

    @Override
    protected final void renderCellBackground(Graphics g, int xCell, int yCell) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;

        // Alternating colors for the board
        if( (xCell + yCell) % 2 == 0) {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, width, height);
        }
    }

    // This function only works properly when coordinates start at one.
    public static final String coordinateToLetter(int x) {
        return "" + ((char) ('a' + x - 1));
    }
}