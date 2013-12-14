package org.ggp.base.util.prover.aima.unifier;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.prover.aima.substitution.Substitution;

public final class Unifier
{

    public static Substitution unify(GdlSentence x, GdlSentence y)
    {
        Substitution theta = new Substitution();
        boolean isGood = unifyTerm(x.toTerm(), y.toTerm(), theta);

        if(isGood)
            return theta;
        else
            return null;
    }

    private static boolean unifyTerm(GdlTerm x, GdlTerm y, Substitution theta)
    {
    	if(x.equals(y))
    		return true;
        if ((x instanceof GdlConstant) && (y instanceof GdlConstant))
        {
            if (!x.equals(y))
            {
                return false;
            }
        }
        else if (x instanceof GdlVariable)
        {
            if (!unifyVariable((GdlVariable) x, y, theta))
                return false;
        }
        else if (y instanceof GdlVariable)
        {
            if (!unifyVariable((GdlVariable) y, x, theta))
                return false;
        }
        else if ((x instanceof GdlFunction) && (y instanceof GdlFunction))
        {
            GdlFunction xFunction = (GdlFunction) x;
            GdlFunction yFunction = (GdlFunction) y;

            if (! unifyTerm(xFunction.getName(), yFunction.getName(), theta))
                return false;

            for (int i = 0; i < xFunction.arity(); i++)
            {
                if (! unifyTerm(xFunction.get(i), yFunction.get(i), theta))
                    return false;
            }
        }
        else
        {
            return false;
        }

        return true;
    }

    private static boolean unifyVariable(GdlVariable var, GdlTerm x, Substitution theta)
    {
        if (theta.contains(var))
        {
            return unifyTerm(theta.get(var), x, theta);
        }
        else if ((x instanceof GdlVariable) && theta.contains((GdlVariable) x))
        {
            return unifyTerm(var, theta.get((GdlVariable) x), theta);
        }
        else
        {
            theta.put(var, x);
            return true;
        }
    }

}
