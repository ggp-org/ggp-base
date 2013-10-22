package org.ggp.base.util.gdl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;


public class GdlUtils {
	//TODO (AL): Check if we can switch over to just having this return a set. 
	public static List<GdlVariable> getVariables(Gdl gdl) {
		final List<GdlVariable> variablesList = new ArrayList<GdlVariable>();
		final Set<GdlVariable> variables = new HashSet<GdlVariable>();
		GdlVisitors.visitAll(gdl, new GdlVisitor() {
			@Override
			public void visitVariable(GdlVariable variable) {
				if (!variables.contains(variable)) {
					variablesList.add(variable);
					variables.add(variable);
				}
			}
		});
		return variablesList;
	}

	public static List<String> getVariableNames(Gdl gdl) {
		List<GdlVariable> variables = getVariables(gdl);
		List<String> variableNames = new ArrayList<String>();
		for (GdlVariable variable : variables) {
			variableNames.add(variable.getName());
		}
		return variableNames;
	}

	public static List<GdlSentence> getSentencesInRuleBody(GdlRule rule) {
		List<GdlSentence> result = new ArrayList<GdlSentence>();
		for(GdlLiteral literal : rule.getBody()) {
			addSentencesInLiteral(literal, result);
		}
		return result;
	}

	private static void addSentencesInLiteral(GdlLiteral literal,
			Collection<GdlSentence> sentences) {
		if(literal instanceof GdlSentence) {
			sentences.add((GdlSentence) literal);
		} else if(literal instanceof GdlNot) {
			GdlNot not = (GdlNot) literal;
			addSentencesInLiteral(not.getBody(), sentences);
		} else if(literal instanceof GdlOr) {
			GdlOr or = (GdlOr) literal;
			for(int i = 0; i < or.arity(); i++)
				addSentencesInLiteral(or.get(i), sentences);
		} else if(!(literal instanceof GdlDistinct)) {
			throw new RuntimeException("Unexpected GdlLiteral type encountered: " + literal.getClass().getSimpleName());
		}
	}

	public static List<GdlTerm> getTupleFromSentence(
			GdlSentence sentence) {
		if(sentence instanceof GdlProposition)
			return Collections.emptyList();
		
		//A simple crawl through the sentence.
		List<GdlTerm> tuple = new ArrayList<GdlTerm>();
		try {
			addBodyToTuple(sentence.getBody(), tuple);
		} catch(RuntimeException e) {
			throw new RuntimeException(e.getMessage() + "\nSentence was " + sentence);
		}
		return tuple;
	}
	private static void addBodyToTuple(List<GdlTerm> body, List<GdlTerm> tuple) {
		for(GdlTerm term : body) {
			if(term instanceof GdlConstant) {
				tuple.add(term);
			} else if(term instanceof GdlVariable) {
				tuple.add(term);
			} else if(term instanceof GdlFunction){
				GdlFunction function = (GdlFunction) term;
				addBodyToTuple(function.getBody(), tuple);
			} else {
				throw new RuntimeException("Unforeseen Gdl tupe in SentenceModel.addBodyToTuple()");
			}
		}
	}

	public static List<GdlConstant> getTupleFromGroundSentence(
			GdlSentence sentence) {
		if(sentence instanceof GdlProposition)
			return Collections.emptyList();
		
		//A simple crawl through the sentence.
		List<GdlConstant> tuple = new ArrayList<GdlConstant>();
		try {
			addBodyToGroundTuple(sentence.getBody(), tuple);
		} catch(RuntimeException e) {
			throw new RuntimeException(e.getMessage() + "\nSentence was " + sentence);
		}
		return tuple;
	}
	private static void addBodyToGroundTuple(List<GdlTerm> body, List<GdlConstant> tuple) {
		for(GdlTerm term : body) {
			if(term instanceof GdlConstant) {
				tuple.add((GdlConstant) term);
			} else if(term instanceof GdlVariable) {
				throw new RuntimeException("Asking for a ground tuple of a non-ground sentence");
			} else if(term instanceof GdlFunction){
				GdlFunction function = (GdlFunction) term;
				addBodyToGroundTuple(function.getBody(), tuple);
			} else {
				throw new RuntimeException("Unforeseen Gdl tupe in SentenceModel.addBodyToTuple()");
			}
		}
	}
	
	public static Map<GdlVariable, GdlConstant> getAssignmentMakingLeftIntoRight(
			GdlSentence left, GdlSentence right) {
		Map<GdlVariable, GdlConstant> assignment = new HashMap<GdlVariable, GdlConstant>();
		if(!left.getName().equals(right.getName()))
			return null;
		if(left.arity() != right.arity())
			return null;
		if(left.arity() == 0)
			return Collections.emptyMap();
		if(!fillAssignmentBody(assignment, left.getBody(), right.getBody()))
			return null;
		return assignment;
	}

	private static boolean fillAssignmentBody(
			Map<GdlVariable, GdlConstant> assignment, List<GdlTerm> leftBody,
			List<GdlTerm> rightBody) {
		//left body contains variables; right body shouldn't
		if(leftBody.size() != rightBody.size()) {
			return false;
		}
		for(int i = 0; i < leftBody.size(); i++) {
			GdlTerm leftTerm = leftBody.get(i);
			GdlTerm rightTerm = rightBody.get(i);
			if(leftTerm instanceof GdlConstant) {
				if(!leftTerm.equals(rightTerm)) {
					return false;
				}
			} else if(leftTerm instanceof GdlVariable) {
				if(assignment.containsKey(leftTerm)) {
					if(!assignment.get(leftTerm).equals(rightTerm)) {
						return false;
					}
				} else {
					if(!(rightTerm instanceof GdlConstant)) {
						return false;
					}
					assignment.put((GdlVariable)leftTerm, (GdlConstant)rightTerm);
				}	
			} else if(leftTerm instanceof GdlFunction) {
				if(!(rightTerm instanceof GdlFunction))
					return false;
				GdlFunction leftFunction = (GdlFunction) leftTerm;
				GdlFunction rightFunction = (GdlFunction) rightTerm;
				if(!leftFunction.getName().equals(rightFunction.getName()))
					return false;
				if(!fillAssignmentBody(assignment, leftFunction.getBody(), rightFunction.getBody()))
					return false;
			}
		}
		return true;
	}

	public static boolean containsTerm(GdlSentence sentence, GdlTerm term) {
		if(sentence instanceof GdlProposition)
			return false;
		return containsTerm(sentence.getBody(), term);
	}

	private static boolean containsTerm(List<GdlTerm> body, GdlTerm term) {
		for(GdlTerm curTerm : body) {
			if(curTerm.equals(term))
				return true;
			if(curTerm instanceof GdlFunction) {
				if(containsTerm(((GdlFunction) curTerm).getBody(), term))
					return true;
			}
		}
		return false;
	}

}
