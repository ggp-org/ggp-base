package org.ggp.base.apps.kiosk.templates;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ggp.base.util.statemachine.MachineState;


/**
 * GameCanvas_FancyGrid expands on the GameCanvas_SimpleGrid model, with
 * additional features based on situations that commonly occur when writing
 * game visualizations. These features make stronger assumptions about the
 * game being visualized, and so may not be appropriate for all games.
 * These assumptions, in addition to the GameCanvas_SimpleGrid assumptions,
 * are as follows:
 * 
 *      1) Each cell of the grid can be drawn independently,
 *         using the following layers:
 *         
 *              a) Background layer, based only on coordinates.
 *              b) Content layer, based only on game propositions.
 *              c) Foreground layer, based only on coordinates.
 *              d) Selection layer, based only on the coordinates
 *                     and the currently selected move.
 *      
 *      2) The game propositions required to render the content layer
 *         of a given cell can be determined from the current state of
 *         the game and the cell's coordinates. As a result, they will
 *         not change unless the game state changes.
 *      
 *      3) Each cell has a certain number of moves associated with it.
 *         These moves can be determined from the current state of the
 *         game and the cell's coordinates. As a result, they will not
 *         change unless the game state changes.
 *         
 *         NOTE: Multiple cells can share the same moves.
 *         
 * Given these assumptions, GameCanvas_FancyGrid handles a lot of the work
 * associated with selecting moves, rendering the grid, and caching useful
 * parts of the game state. The player will be able to select their move by
 * clicking on a cell: if there are multiple moves associated with the cell,
 * they can click on the cell multiple times to cycle through them.
 * 
 * To reflect the above assumptions, GameCanvas_FancyGrid overrides the
 * andleClickOnCell() and clearMoveSelection() methods, and instead provides
 * the following abstract methods:
 * 
 *      getFactsAboutCell(x,y)           Get all facts associated with a cell.
 *      getLegalMovesForCell(x,y)        Get all legal moves associated with a cell.
 *      
 *      renderCellBackground(g,x,y)      Draw the background for a cell.
 *      renderCellContent(g,fact)        Draw the content for a cell, given its fact.
 *      renderCellContent(g,facts)       Draw the content for a cell, given its facts.
 *      renderCellForeground(g,x,y)      Draw the foreground for a cell.
 *      
 *      renderMoveSelectionForCell(g,x,y,move)      Draw the move selection for a cell.
 * 
 * One common situation involves each cell having exactly one fact associated
 * with it, often of the form "(cell 1 1 x)". In this situation, it's more convenient
 * to have that single fact extracted and passed to renderCellContent() as a String,
 * rather than as a Set<String> containing one element. If you want that automatic
 * extraction done, override the renderCellContent() method that takes a String as its
 * second parameter. Otherwise, if your getFactsAboutCell() method is expected to return
 * multiple facts about the same cell, override the renderCellContent() method that takes
 * a Set<String> as its second parameter.
 * 
 * Another common situation involves having a game where you want to automatically
 * display the grid, drawing black boxes around grid cells and always highlighting
 * the selected grid cell with a green border. This grid visualization will be done
 * by default. If you want to disable it, override the "useGridVisualization" function
 * so that it returns false.
 * 
 * @author Sam Schreiber
 */
public abstract class GameCanvas_FancyGrid extends GameCanvas_SimpleGrid {
    private static final long serialVersionUID = 1L;

    protected abstract Set<String> getFactsAboutCell(int xCell, int yCell);
    protected abstract Set<String> getLegalMovesForCell(int xCell, int yCell);
        
    protected void renderCellBackground(Graphics g, int xCell, int yCell) {};
    protected void renderCellForeground(Graphics g, int xCell, int yCell) {};
    protected void renderMoveSelectionForCell(Graphics g, int xCell, int yCell, String theMove) {};
    
    protected void renderCellContent(Graphics g, String theFact) {};
    protected void renderCellContent(Graphics g, Set<String> theFacts){
        if(theFacts.size() > 0) {
            if(theFacts.size() > 1) {
                System.err.println("More than one fact for a cell? Unexpected!");
            }

            String theFact = theFacts.iterator().next();
            renderCellContent(g, theFact);
        }        
    }
    
    protected boolean useGridVisualization() { return true; }
    
    protected final boolean isSelectedCell(int xCell, int yCell) {
        return (yCell == selectedRow && xCell == selectedColumn);
    }
    
    private int selectedRow = -1;
    private int selectedColumn = -1; 
    private String currentSelectedMove;
    private Iterator<String> possibleSelectedMoves = null;
    protected final void handleClickOnCell(int xCell, int yCell, int xWithin, int yWithin) {
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
        if(useGridVisualization()) CommonGraphics.drawCellBorder(g);
        renderCellForeground(g, xCell, yCell);
        if(!currentSelectedMove.isEmpty()) {                        
            renderMoveSelectionForCell(g, xCell, yCell, currentSelectedMove);
            if(useGridVisualization() && isSelectedCell(xCell, yCell))
                CommonGraphics.drawSelectionBox(g);
        }
    }

    public final void clearMoveSelection() {        
        submitWorkingMove(null);
        
        possibleSelectedMoves = null;
        currentSelectedMove = "";
        selectedColumn = -1;    
        selectedRow = -1;
        
        repaint();
    }    
}