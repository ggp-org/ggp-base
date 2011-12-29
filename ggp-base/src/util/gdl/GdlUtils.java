package util.gdl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class GdlUtils {
	public static List<GdlVariable> getVariables(GdlRule rule) {
		List<String> variableNames = getVariableNames(rule);
		List<GdlVariable> variables = new ArrayList<GdlVariable>();
		for(String name : variableNames)
			variables.add(GdlPool.getVariable(name));
		return variables;
	}
	public static List<GdlVariable> getVariables(GdlLiteral literal) {
		List<String> variableNames = getVariableNames(literal);
		List<GdlVariable> variables = new ArrayList<GdlVariable>();
		for(String name : variableNames)
			variables.add(GdlPool.getVariable(name));
		return variables;
	}
	//I happened to have written the name-finding code first; the other way around would
	//probably be more efficient. The use of a list over a set is also for historical
	//reasons, though some applications could make use of the consistent ordering.
	//TODO: Switch to the Gdl-based version as the underlying implementation.
	public static List<String> getVariableNames(GdlLiteral literal) {
		List<String> varNames = new ArrayList<String>();
		addVariableNames(varNames, literal);
		return varNames;
	}
	public static List<String> getVariableNames(GdlRule rule) {
		List<String> varNames = new ArrayList<String>();
		addVariableNames(varNames, rule.getHead());
		for(GdlLiteral conjunct : rule.getBody())
			addVariableNames(varNames, conjunct);
		return varNames;
	}
	public static void addVariableNames(List<String> variables, GdlLiteral literal) {
		if(literal instanceof GdlRelation) {
			GdlSentence sentence = (GdlSentence) literal;
			addVariableNames(variables, sentence.getBody());
		} else if(literal instanceof GdlOr) {
			GdlOr or = (GdlOr) literal;
			for(int i = 0; i < or.arity(); i++)
				addVariableNames(variables, or.get(i));
		} else if(literal instanceof GdlNot) {
			GdlNot not = (GdlNot) literal;
			addVariableNames(variables, not.getBody());
		} else if(literal instanceof GdlDistinct) {
			GdlDistinct distinct = (GdlDistinct) literal;
			List<GdlTerm> pair = new ArrayList<GdlTerm>(2);
			pair.add(distinct.getArg1());
			pair.add(distinct.getArg2());
			addVariableNames(variables, pair);
		} else if(literal instanceof GdlProposition) {
			//No variables
		} else {
			throw new RuntimeException("Unforeseen literal type");
		}
	}
	private static void addVariableNames(List<String> variables, List<GdlTerm> body) {
		for(GdlTerm term : body) {
			if(term instanceof GdlVariable) {
				GdlVariable var = (GdlVariable) term;
				if(!variables.contains(var.getName()))
					variables.add(var.getName());
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				addVariableNames(variables, function.getBody());
			}
		}
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
