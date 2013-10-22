package org.ggp.base.util.statemachine;

import java.io.Serializable;

import org.ggp.base.util.gdl.grammar.GdlTerm;


@SuppressWarnings("serial")
public class Move implements Serializable
{
    protected final GdlTerm contents;

    public Move(GdlTerm contents)
    {
        this.contents = contents;
    }

    @Override
    public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof Move)) {
            Move move = (Move) o;
            return move.contents.equals(contents);
        }

        return false;
    }

    public GdlTerm getContents()
    {
        return contents;
    }

    @Override
    public int hashCode()
    {
        return contents.hashCode();
    }

    @Override
    public String toString()
    {
        return contents.toString();
    }
}