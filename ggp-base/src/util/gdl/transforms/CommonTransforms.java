package util.gdl.transforms;

import java.util.ArrayList;
import java.util.List;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlOr;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;

/**
 * @author Sam Schreiber
 */
public class CommonTransforms {
	//We can avoid lots of client-side casts by providing these functions for the more specific cases -AL
	public static GdlRule replaceVariable(GdlRule rule, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlRule) replaceVariableInternal(rule, toSubstitute, theReplacement);
	}
	public static GdlLiteral replaceVariable(GdlLiteral literal, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlLiteral) replaceVariableInternal(literal, toSubstitute, theReplacement);
	}
	public static GdlSentence replaceVariable(GdlSentence sentence, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlSentence) replaceVariableInternal(sentence, toSubstitute, theReplacement);
	}
	public static GdlFunction replaceVariable(GdlFunction function, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlFunction) replaceVariableInternal(function, toSubstitute, theReplacement);
	}
	public static GdlNot replaceVariable(GdlNot not, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlNot) replaceVariableInternal(not, toSubstitute, theReplacement);
	}
	public static GdlOr replaceVariable(GdlOr or, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlOr) replaceVariableInternal(or, toSubstitute, theReplacement);
	}
	public static GdlDistinct replaceVariable(GdlDistinct distinct, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlDistinct) replaceVariableInternal(distinct, toSubstitute, theReplacement);
	}
	public static GdlRelation replaceVariable(GdlRelation relation, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlRelation) replaceVariableInternal(relation, toSubstitute, theReplacement);
	}
	public static GdlTerm replaceVariable(GdlTerm term, GdlVariable toSubstitute, GdlTerm theReplacement) {
		return (GdlTerm) replaceVariableInternal(term, toSubstitute, theReplacement);
	}
    private static Gdl replaceVariableInternal(Gdl gdl, GdlVariable toSubstitute, GdlTerm theReplacement) {
        if(gdl instanceof GdlDistinct) {
            return GdlPool.getDistinct((GdlTerm) replaceVariableInternal(((GdlDistinct) gdl).getArg1(), toSubstitute, theReplacement), (GdlTerm) replaceVariableInternal(((GdlDistinct) gdl).getArg2(), toSubstitute, theReplacement));
        } else if(gdl instanceof GdlNot) {
            return GdlPool.getNot((GdlLiteral) replaceVariableInternal(((GdlNot) gdl).getBody(), toSubstitute, theReplacement));
        } else if(gdl instanceof GdlOr) {
            GdlOr or = (GdlOr)gdl;
            List<GdlLiteral> rval = new ArrayList<GdlLiteral>();
            for(int i=0; i<or.arity(); i++)
            {
                rval.add((GdlLiteral) replaceVariableInternal(or.get(i), toSubstitute, theReplacement));                
            }
            return GdlPool.getOr(rval);
        } else if(gdl instanceof GdlProposition) {
            return gdl;
        } else if(gdl instanceof GdlRelation) {
            GdlRelation rel = (GdlRelation)gdl;
            List<GdlTerm> rval = new ArrayList<GdlTerm>();
            for(int i=0; i<rel.arity(); i++)
            {
                rval.add((GdlTerm) replaceVariableInternal(rel.get(i), toSubstitute, theReplacement));                
            }   
            return GdlPool.getRelation(rel.getName(), rval);
        } else if(gdl instanceof GdlRule) {
            GdlRule rule = (GdlRule)gdl;
            List<GdlLiteral> rval = new ArrayList<GdlLiteral>();
            for(int i=0; i<rule.arity(); i++)
            {
                rval.add((GdlLiteral) replaceVariableInternal(rule.get(i), toSubstitute, theReplacement));                
            }
            return GdlPool.getRule((GdlSentence) replaceVariableInternal(rule.getHead(), toSubstitute, theReplacement), rval);            
        } else if(gdl instanceof GdlConstant) {
            return gdl;
        } else if(gdl instanceof GdlFunction) {
            GdlFunction func = (GdlFunction)gdl;
            List<GdlTerm> rval = new ArrayList<GdlTerm>();
            for(int i=0; i<func.arity(); i++)
            {
                rval.add((GdlTerm) replaceVariableInternal(func.get(i), toSubstitute, theReplacement));                
            }   
            return GdlPool.getFunction(func.getName(), rval);
        } else if(gdl instanceof GdlVariable) {
            if(gdl == toSubstitute) {
                return theReplacement;
            } else {
                return gdl;
            }
        } else {
            throw new RuntimeException("Uh oh, gdl hierarchy must have been extended without updating this code.");
        }
    }    
}