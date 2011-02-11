package util.statemachine;

import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;

public abstract class MachineState {
    /* Abstract methods */
    
    /**
     * getContents returns the GDL sentences which determine the current state
     * of the game being played. Two given states with identical GDL sentences
     * should be identical states of the game.
     */
	public abstract Set<GdlSentence> getContents();

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
    public final String toMatchXML()
    {
        String rval = "<match>\n <herstory>\n";
        rval += toXML();
        rval += " </herstory>\n</match>";
        return rval;
    }
    
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