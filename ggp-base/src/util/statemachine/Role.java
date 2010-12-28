package util.statemachine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.statemachine.Role;

@SuppressWarnings("serial")
public class Role implements Serializable
{
    protected final GdlProposition name;

    public Role(GdlProposition name)
    {
        this.name = name;
    }

    @Override
    public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof Role))
        {
            Role role = (Role) o;
            return role.name.equals(name);
        }

        return false;
    }

    public GdlProposition getName()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return name.toString();
    }
    
    /**
     * Compute all of the roles in a game, in the correct order.
     * 
     * Order matters, because a joint move is defined as an ordered list
     * of moves, in which the order determines which player took which of
     * the moves. This function will give an ordered list in which the roles
     * have that correct order.
     */
    public static List<Role> computeRoles(List<? extends Gdl> description)
    {
        List<Role> roles = new ArrayList<Role>();
        for (Gdl gdl : description) {
            if (gdl instanceof GdlRelation) {
                //TODO: check if things like ( role ?player ) are legal
                GdlRelation relation = (GdlRelation) gdl;               
                if (relation.getName().getValue().equals("role")) {
                    roles.add(new Role((GdlProposition) relation.get(0).toSentence()));
                }
            }
        }
        return roles;
    }    
}