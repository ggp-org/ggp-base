package org.ggp.base.util.statemachine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;


@SuppressWarnings("serial")
public class Role implements Serializable
{
    protected final GdlConstant name;

    public Role(GdlConstant name)
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

    public GdlConstant getName()
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
                GdlRelation relation = (GdlRelation) gdl;               
                if (relation.getName().getValue().equals("role")) {
                    roles.add(new Role((GdlConstant) relation.get(0)));
                }
            }
        }
        return roles;
    }    
}