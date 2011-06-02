package util.statemachine;

import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;

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
    
    // TODO: Do we really need this method?
    // Should we update it to something more JSON-friendly?
    public final String toXML()
    {
        String rval = "<state>\n";
        Set<GdlSentence> theContents = getContents();
        for(GdlSentence sentence : theContents)
        {
            rval += gdlToXML(sentence);
        }
        rval += "</state>";
        return rval;
    }
    
    // TODO: Do we really need this method?
    // Should we update it to something more JSON-friendly?    
    public final String toMatchXML()
    {
        String rval = "<match>\n <herstory>\n";
        rval += toXML();
        rval += " </herstory>\n</match>";
        return rval;
    }
    
    // TODO: Do we really need this method?
    // Should we update it to something more JSON-friendly?    
    private final String gdlToXML(Gdl gdl)
    {
        String rval = "";
        if(gdl instanceof GdlConstant)
        {
            GdlConstant c = (GdlConstant)gdl;
            return c.getValue();
        } else if(gdl instanceof GdlFunction) {
            GdlFunction f = (GdlFunction)gdl;
            if(f.getName().toString().equals("true"))
            {
                return "\t<fact>\n"+gdlToXML(f.get(0))+"\t</fact>\n";
            }
            else
            {
                rval += "\t\t<relation>"+f.getName()+"</relation>\n";
                for(int i=0; i<f.arity(); i++)
                    rval += "\t\t<argument>"+gdlToXML(f.get(i))+"</argument>\n";
                return rval;
            }
        } else if (gdl instanceof GdlRelation) {
            GdlRelation relation = (GdlRelation) gdl;
            if(relation.getName().toString().equals("true"))
            {
                for(int i=0; i<relation.arity(); i++)
                    rval+="\t<fact>\n"+gdlToXML(relation.get(i))+"\t</fact>\n";
                return rval;
            } else {
                rval+="\t\t<relation>"+relation.getName()+"</relation>\n";
                for(int i=0; i<relation.arity(); i++)
                    rval+="\t\t<argument>"+gdlToXML(relation.get(i))+"</argument>\n";
                return rval;
            }
        } else {
            System.err.println("MachineState gdlToXML Error: could not handle "+gdl.toString());
            return "";
        }
    }
}