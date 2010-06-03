package apps.kiosk.games;

import java.util.HashSet;
import java.util.Set;

public class BlokboxSimpleCanvas extends BlokboxDuoCanvas {
    private static final long serialVersionUID = 1L;

    public String getGameName() { return "Blokbox Simple"; }
    protected String getGameKIF() { return "blokbox_simple"; }
    
    protected Set<String> getLegalMovesForCell(int xCell, int yCell) {
        if(selectedPiece == -1) return new HashSet<String>();
        return gameStateHasLegalMovesMatching("\\( place " + selectedPiece + " " + xCell + " " + yCell + " \\)");
    }
}