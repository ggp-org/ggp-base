package org.ggp.base.apps.kiosk;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;

public class MoveSelectedEvent extends Event {
    private Move theMove;
    private boolean isFinal = false;
    
    public MoveSelectedEvent(Move m) {
        theMove = m;
    }
    
    public MoveSelectedEvent(Move m, boolean isFinal) {
    	theMove = m;
    	this.isFinal = isFinal;
    }
    
    public Move getMove() {
        return theMove;
    }
    
    public boolean isFinal() {
    	return isFinal;
    }
}