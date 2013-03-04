package org.ggp.base.util.statemachine;

import java.util.Set;
import org.ggp.base.util.gdl.grammar.GdlSentence;

public class MachineState {
    public MachineState() {
        this.contents = null;
    }
    
    /**
     * Starts with a simple implementation of a MachineState. StateMachines that
     * want to do more advanced things can subclass this implementation, but for
     * many cases this will do exactly what we want.
     */
    private final Set<GdlSentence> contents;
    public MachineState(Set<GdlSentence> contents)
    {
        this.contents = contents;
    }

    /**
     * getContents returns the GDL sentences which determine the current state
     * of the game being played. Two given states with identical GDL sentences
     * should be identical states of the game.
     */
	public Set<GdlSentence> getContents()
	{
        return contents;
    }

	/* Utility methods */
    public int hashCode()
    {
        return getContents().hashCode();
    }

    public String toString()
    {
    	Set<GdlSentence> contents = getContents();
    	if(contents == null)
    		return "(MachineState with null contents)";
    	else
    		return contents.toString();
    }	

    public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof MachineState))
        {
            MachineState state = (MachineState) o;
            return state.getContents().equals(getContents());
        }

        return false;
    }
}