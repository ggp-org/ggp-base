package org.ggp.base.util.gdl.transforms;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlVariable;


/**
 * As a GDL transformer, this class takes in a GDL description of a game,
 * transforms it in some way, and outputs a new GDL descriptions of a game
 * which is functionally equivalent to the original game.
 * 
 * DeORer removes OR rules from the GDL. Technically, these rules shouldn't
 * be in the GDL in the first place, but it's very straightforward to remove
 * them, so we do that so that we can handle GDL descriptions that use OR.
 * 
 * @author Ethan Dreyfuss
 */
public class DeORer {
    public static List<Gdl> run(List<Gdl> description)
    {
        List<Gdl> newDesc = new ArrayList<Gdl>();
        for(Gdl gdl : description)
        {
            if(gdl instanceof GdlRule)
            {
                GdlRule rule = (GdlRule)gdl;
                List<List<GdlLiteral>> newBodies = deOr(rule.getBody());
                for(List<GdlLiteral> body : newBodies)
                {
                    newDesc.add(GdlPool.getRule(rule.getHead(), body));
                }
            }
            else
                newDesc.add(gdl);
        }
        return newDesc;
    }
    
    private static List<List<GdlLiteral>> deOr(List<GdlLiteral> rhs) {
        List<List<GdlLiteral>> wrapped = new ArrayList<List<GdlLiteral>>();
        wrapped.add(rhs);
        return deOr2(wrapped);
    }

    private static List<List<GdlLiteral>> deOr2(List<List<GdlLiteral>> rhsList) {
        List<List<GdlLiteral>> rval = new ArrayList<List<GdlLiteral>>();
        boolean expandedSomething = false;
        for(List<GdlLiteral> rhs : rhsList)
        {
            int i=0;
            if(!expandedSomething)
            {
                for(GdlLiteral lit : rhs)
                {
                    if(!expandedSomething)
                    {
                        List<Gdl> expandedList = expandFirstOr(lit);
                        
                        if(expandedList.size() > 1)
                        {
                            for(Gdl replacement : expandedList)
                            {                   
                                List<GdlLiteral> newRhs = new ArrayList<GdlLiteral>(rhs);
                                if(!(replacement instanceof GdlLiteral)) throw new RuntimeException("Top level return value is different type of gdl.");
                                GdlLiteral newLit = (GdlLiteral)replacement;
                                newRhs.set(i, newLit);
                                rval.add(newRhs);
                            }
                            expandedSomething = true;
                            break;                      
                        }
                    }
                    
                    i++;
                }
                if(!expandedSomething) //If I didn't find anything to expand
                    rval.add(rhs);
            }
            else
                rval.add(rhs); //If I've already expanded this function call
            
        }
        
        if(!expandedSomething)      
            return rhsList;
        else
            return deOr2(rval);
    }

    private static List<Gdl> expandFirstOr(Gdl gdl) {
        List<Gdl> rval;
        List<Gdl> expandedChild;
        if(gdl instanceof GdlDistinct)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlNot)
        {
            GdlNot not = (GdlNot)gdl;
            expandedChild = expandFirstOr(not.getBody());
            rval = new ArrayList<Gdl>();
            for(Gdl g : expandedChild)
            {
                if(!(g instanceof GdlLiteral)) throw new RuntimeException("Not must have literal child.");
                GdlLiteral lit = (GdlLiteral)g;
                rval.add(GdlPool.getNot(lit));
            }
            return rval;
        }
        else if(gdl instanceof GdlOr)
        {
            GdlOr or = (GdlOr)gdl;
            rval = new ArrayList<Gdl>();
            for(int i=0; i<or.arity(); i++)
            {
                rval.add(or.get(i));                
            }
            return rval;
        }
        else if(gdl instanceof GdlProposition)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlRelation)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlRule)
        {
            throw new RuntimeException("This should be used to remove 'or's from the body of a rule, and rules can't be nested");
        }
        else if(gdl instanceof GdlConstant)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlFunction)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlVariable)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else
        {
            throw new RuntimeException("Uh oh, gdl hierarchy must have been extended without updating this code.");
        }
    }
}
