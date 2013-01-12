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
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;


//Cleans up various issues with games to make them more standardized.
public class GdlCleaner {

	public static List<Gdl> run(List<Gdl> description) {
		List<Gdl> newDescription = new ArrayList<Gdl>();
		
		//First: Clean up all rules with zero-element bodies
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				if(rule.getBody().size() == 0) {
					newDescription.add(rule.getHead());
				} else {
					newDescription.add(gdl);
				}
			} else {
				newDescription.add(gdl);
			}
		}
		
		//TODO: Add (role ?player) where appropriate, i.e. in rules for
		//"legal" or "input" where the first argument is an undefined
		//variable
		//Get rid of "extra parentheses", i.e. zero-arity functions
		description = newDescription;
		newDescription = new ArrayList<Gdl>();
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRelation) {
				newDescription.add(cleanParentheses((GdlRelation)gdl));
			} else if(gdl instanceof GdlRule) {
				newDescription.add(cleanParentheses((GdlRule)gdl));
			} else {
				newDescription.add(gdl);
			}
		}
		//TODO: Get rid of GdlPropositions in the description
		
		//Get rid of (not (distinct _ _)) literals in rules
		//TODO: Expand to functions
		description = newDescription;
		newDescription = new ArrayList<Gdl>();
		for(Gdl gdl : description) {
		    if(gdl instanceof GdlRule) {
		        GdlRule cleaned = removeNotDistinctLiterals((GdlRule)gdl);
		        if(cleaned != null)
		            newDescription.add(cleaned);
		    } else {
		        newDescription.add(gdl);
		    }
		}
		
		return newDescription;
	}

	private static GdlRule removeNotDistinctLiterals(GdlRule rule) {
        while(rule != null && getNotDistinctLiteral(rule) != null) {
            rule = removeNotDistinctLiteral(rule, getNotDistinctLiteral(rule));
        }
        return rule;
    }

    private static GdlNot getNotDistinctLiteral(GdlRule rule) {
        for(GdlLiteral literal : rule.getBody()) {
            if(literal instanceof GdlNot) {
                GdlNot not = (GdlNot) literal;
                if(not.getBody() instanceof GdlDistinct) {
                    //For now, we can only deal with this if not both are functions.
                    //That means we have to skip that case at this point.
                    GdlDistinct distinct = (GdlDistinct) not.getBody();
                    if(!(distinct.getArg1() instanceof GdlFunction)
                            || !(distinct.getArg2() instanceof GdlFunction))
                        return not;
                }
            }
        }
        return null;
    }

    //Returns null if the rule is useless.
    private static GdlRule removeNotDistinctLiteral(GdlRule rule, GdlNot notDistinctLiteral) {
        //Figure out the substitution we want...
        //If we have two constants: Either remove one or
        //maybe get rid of the ___?
        //One is a variable: Replace the variable with the other thing
        //throughout the rule
        GdlDistinct distinct = (GdlDistinct) notDistinctLiteral.getBody();
        GdlTerm arg1 = distinct.getArg1();
        GdlTerm arg2 = distinct.getArg2();
        if(arg1 == arg2) {
            //Just remove that literal
            List<GdlLiteral> newBody = new ArrayList<GdlLiteral>();
            newBody.addAll(rule.getBody());
            newBody.remove(notDistinctLiteral);
            return GdlPool.getRule(rule.getHead(), newBody);
        }
        if(arg1 instanceof GdlVariable) {
            //What we return will still have the not-distinct literal,
            //but it will get replaced in the next pass.
            //(Even if we have two variables, they will be equal next time through.)
            return CommonTransforms.replaceVariable(rule, (GdlVariable)arg1, arg2);
        }
        if(arg2 instanceof GdlVariable) {
            return CommonTransforms.replaceVariable(rule, (GdlVariable)arg2, arg1);
        }
        if(arg1 instanceof GdlConstant || arg2 instanceof GdlConstant) {
            //We have two non-equal constants, or a constant and a function.
            //The rule should have no effect.
            return null;
        }
        //We have two functions. Complicated! (Have to replace them with unified version.)
        //We pass on this case for now.
        //TODO: Implement correctly.
        throw new UnsupportedOperationException("We can't currently handle (not (distinct <function> <function>)).");
    }

    private static GdlRule cleanParentheses(GdlRule rule) {
		GdlSentence cleanedHead = cleanParentheses(rule.getHead());
		List<GdlLiteral> cleanedBody = new ArrayList<GdlLiteral>();
		for(GdlLiteral literal : rule.getBody())
			cleanedBody.add(cleanParentheses(literal));
		return GdlPool.getRule(cleanedHead, cleanedBody);
	}

	private static GdlLiteral cleanParentheses(GdlLiteral literal) {
		if(literal instanceof GdlSentence) {
			return cleanParentheses((GdlSentence)literal);
		} else if(literal instanceof GdlDistinct) {
			GdlDistinct distinct = (GdlDistinct) literal;
			GdlTerm term1 = cleanParentheses(distinct.getArg1());
			GdlTerm term2 = cleanParentheses(distinct.getArg2());
			return GdlPool.getDistinct(term1, term2);
		} else if(literal instanceof GdlNot) {
			return GdlPool.getNot(((GdlNot) literal).getBody());
		} else if(literal instanceof GdlOr) {
			GdlOr or = (GdlOr) literal;
			List<GdlLiteral> disjuncts = new ArrayList<GdlLiteral>();
			for(int i = 0; i < or.arity(); i++)
				disjuncts.add(cleanParentheses(or.get(i)));
			return GdlPool.getOr(disjuncts);
		}
		throw new RuntimeException("Unexpected literal type in GdlCleaner");
	}

	private static GdlSentence cleanParentheses(GdlSentence sentence) {
		if(sentence instanceof GdlProposition)
			return sentence;
		List<GdlTerm> cleanedBody = new ArrayList<GdlTerm>();
		for(GdlTerm term : sentence.getBody())
			cleanedBody.add(cleanParentheses(term));
		return GdlPool.getRelation(sentence.getName(), cleanedBody);
	}

	private static GdlTerm cleanParentheses(GdlTerm term) {
		if(term instanceof GdlConstant || term instanceof GdlVariable)
			return term;
		if(term instanceof GdlFunction) {
			GdlFunction function = (GdlFunction) term;
			//The whole point of the function
			if(function.arity() == 0)
				return function.getName();
			List<GdlTerm> cleanedBody = new ArrayList<GdlTerm>();
			for(GdlTerm functionTerm : function.getBody())
				cleanedBody.add(cleanParentheses(functionTerm));
			return GdlPool.getFunction(function.getName(), cleanedBody);
		}
		throw new RuntimeException("Unexpected term type in GdlCleaner");
	}
}
