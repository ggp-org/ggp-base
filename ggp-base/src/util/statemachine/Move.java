package util.statemachine;

import java.io.Serializable;

import util.gdl.grammar.GdlSentence;

@SuppressWarnings("serial")
public class Move implements Serializable
{
    protected final GdlSentence contents;

    public Move(GdlSentence contents)
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

    public GdlSentence getContents()
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