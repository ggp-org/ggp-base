package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import apps.kiosk.templates.CommonGraphics;
import apps.kiosk.templates.GameCanvas_FancyGrid;

// NOTE: UNDER DEVELOPMENT
public class BlokboxDuoCanvas extends GameCanvas_FancyGrid {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Blokbox Duo"; }
    protected String getGameKIF() { return "blokbox_duo"; }
    protected int getGridHeight() { return 14; }
    protected int getGridWidth() { return 14; }
    
    @Override
    protected Set<String> getFactsAboutCell(int xCell, int yCell) {
        return gameStateHasFactsMatching("\\( cell " + xCell + " " + yCell + " (.*) \\)");
    }
    
    @Override
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        return gameStateHasLegalMovesMatching("\\( place (.*) " + xCell + " " + yCell + " \\)");
    }
    
    @Override
    protected void renderCellContent(Graphics g, String theFact) {
        String[] cellFacts = theFact.split(" ");
        String cellPlayer = cellFacts[4];
        if (cellPlayer.equals("orange")) {
            g.setColor(Color.ORANGE);
        } else if (cellPlayer.equals("purple")) {
            g.setColor(Color.MAGENTA);
        }
        CommonGraphics.drawDisc(g);
    }
    
    @Override
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {
        /*
        if(isSelectedCell(xCell, yCell)) {
            String[] moveParts = theMove.split(" ");
            int captureMask = Integer.parseInt(moveParts[5]);
            renderCaptureMask(g, captureMask);
        }
        */
    }
}