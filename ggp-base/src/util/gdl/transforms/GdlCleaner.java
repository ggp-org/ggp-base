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
					//System.out.println("Changing rule " + rule + " into relation " + rule.getHead());
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
		
		return newDescription;
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
