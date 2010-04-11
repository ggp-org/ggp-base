package apps.kiosk.templates;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import util.statemachine.MachineState;

public abstract class StyledGameCanvas_FancyRectangles extends StyledGameCanvas_Rectangles {
    public static final long serialVersionUID = 0x1;
    
    protected abstract Set<String> getFactsAboutRectangle(int nRectangle);
    protected abstract Set<String> getLegalMovesForRectangle(int nRectangle);
        
    protected void renderRectangleBackground(Graphics g, int nRectangle) {};
    protected void renderRectangleForeground(Graphics g, int nRectangle) {};
    protected void renderMoveSelectionForRectangle(Graphics g, int nRectangle, String theMove) {};
    
    protected boolean useRectangleVisualization() { return false; }
    
    protected void renderRectangleContent(Graphics g, String theFact) {};
    protected void renderRectangleContent(Graphics g, Set<String> theFacts){
        if(theFacts.size() > 0) {
            if(theFacts.size() > 1) {
                System.err.println("More than one fact for a cell? Unexpected!");
            }

            String theFact = theFacts.iterator().next();
            renderRectangleContent(g, theFact);
        }        
    }
    
    protected final boolean isSelectedRectangle(int nRectangle) {
        return (nRectangle == selectedRectangle);
    }
    
    private int selectedRectangle = -1;
    private String currentSelectedMove;
    private Iterator<String> possibleSelectedMoves = null;
    protected final void handleClickOnRectangle(int nRectangle) {
        if(selectedRectangle != nRectangle || !possibleSelectedMoves.hasNext()) {
            SortedSet<String> theMoves = new TreeSet<String>(getLegalMovesForRectangle(nRectangle));
            if(theMoves.size() == 0)
                return;
            possibleSelectedMoves = theMoves.iterator();            
        }
        
        selectedRectangle = nRectangle;
        
        currentSelectedMove = possibleSelectedMoves.next();        
        submitWorkingMove(stringToMove(currentSelectedMove));
    }

    // Cache all of the facts about cells that we compute, since they should not
    // change unless the game state changes.
    private Map<Integer, Set<String>> factsCache = new HashMap<Integer, Set<String>>();
    protected Set<String> getCachedFactsAboutRectangle(int nRectangle) {
        Set<String> cachedFacts = factsCache.get(nRectangle);
        if(cachedFacts != null)
            return cachedFacts;
        
        Set<String> realFacts = getFactsAboutRectangle(nRectangle);
        factsCache.put(nRectangle, realFacts);
        return realFacts;
    }
    
    // When the game state changes, clear our cache of known facts.
    public void updateGameState(MachineState gameState) {
        factsCache.clear();
        super.updateGameState(gameState);
    }
    
    protected final void renderRectangle(Graphics g, int nRectangle) {
        renderRectangleBackground(g, nRectangle);
        renderRectangleContent(g, getCachedFactsAboutRectangle(nRectangle));
        if(useRectangleVisualization()) CommonGraphics.drawCellBorder(g);
        renderRectangleForeground(g, nRectangle);
        if(!currentSelectedMove.isEmpty()) {                        
            renderMoveSelectionForRectangle(g, nRectangle, currentSelectedMove);
            if(useRectangleVisualization() && isSelectedRectangle(nRectangle))
                CommonGraphics.drawSelectionBox(g);
        }
    }

    public final void clearMoveSelection() {        
        submitWorkingMove(null);
        
        possibleSelectedMoves = null;
        currentSelectedMove = "";
        selectedRectangle = -1;
        
        repaint();
    }
}