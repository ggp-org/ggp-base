package util.gdl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
 * A model of all the sentences that can be formed (loosely speaking) in a particular game.
 * For each relation and function, this model contains the values (both constants and functions)
 * that each of its terms may have, according to some simple analysis of the rules. The values
 * found are a superset of the actual values that will be found in the game; not all of these
 * values are necessarily reachable in the actual game.
 * 
 * @author Alex Landau
 */
public class SentenceModel {
	private Map<String, List<TermModel>> sentences = new HashMap<String, List<TermModel>>();
	
	public SentenceModel(List<Gdl> description) {
		List<GdlRule> rules = new ArrayList<GdlRule>();
		//First, get all the constants (non-rules) into the model
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRelation) {
				GdlRelation relation = (GdlRelation) gdl;
				addToModel(relation);
			} else if(gdl instanceof GdlRule) {
				rules.add((GdlRule) gdl);
			} else {
				throw new RuntimeException("Description contains non-relation, non-rule Gdl " + gdl + " of class " + gdl.getClass());
			}
		}

		//For the purposes of the language-based injections, we need
		//these to be included in the model at this point
		if(!sentences.containsKey("legal_2")) {
			List<TermModel> termsList = new ArrayList<TermModel>(2);
			termsList.add(new TermModel());
			termsList.add(new TermModel());
			sentences.put("legal_2", termsList);
		}
		if(!sentences.containsKey("does_2")) {
			List<TermModel> termsList = new ArrayList<TermModel>(2);
			termsList.add(new TermModel());
			termsList.add(new TermModel());
			sentences.put("does_2", termsList);
		}
		if(!sentences.containsKey("init_1")) {
			List<TermModel> termsList = new ArrayList<TermModel>(1);
			termsList.add(new TermModel());
			sentences.put("init_1", termsList);
		}
		if(!sentences.containsKey("next_1")) {
			List<TermModel> termsList = new ArrayList<TermModel>(1);
			termsList.add(new TermModel());
			sentences.put("next_1", termsList);
		}
		if(!sentences.containsKey("true_1")) {
			List<TermModel> termsList = new ArrayList<TermModel>(1);
			termsList.add(new TermModel());
			sentences.put("true_1", termsList);
		}
		
		//Now, apply injections through rules until we've gotten absolutely everything
		//The brute-force method is to repeatedly apply injection at every rule until nothing new is added
		//GDL descriptions are generally small enough that this should work
		boolean somethingChanged = true;
		while(somethingChanged) {
			somethingChanged = false;
			if(applyLanguageInjections())
				somethingChanged = true;
			for(GdlRule rule : rules) {
				//We apply the injection, and note if it changes the model
				if(applyInjection(rule))
					somethingChanged = true;
			}
		}
	}
	
	private boolean applyLanguageInjections() {
		//Injects init and next to true, and legal to does.
		boolean somethingChanged = false;
		
		List<TermModel> legalBody = sentences.get("legal_2");
		List<TermModel> doesBody = sentences.get("does_2");
		List<TermModel> initBody = sentences.get("init_1");
		List<TermModel> nextBody = sentences.get("next_1");
		List<TermModel> trueBody = sentences.get("true_1");
		
		if(doesBody.get(0).inject(legalBody.get(0)))
			somethingChanged = true;
		if(doesBody.get(1).inject(legalBody.get(1)))
			somethingChanged = true;
		if(trueBody.get(0).inject(initBody.get(0)))
			somethingChanged = true;
		if(trueBody.get(0).inject(nextBody.get(0)))
			somethingChanged = true;

		return somethingChanged;
	}

	private boolean applyInjection(GdlRule rule) {
		boolean result = false;
		boolean somethingChanged = true;
		while(somethingChanged) {
			somethingChanged = false;
			
			//For each variable in the LHS of the rule, we want
			//to get all the possible values implied by each
			//positive conjunct on the RHS.
			GdlSentence head = rule.getHead();
			
			//This preliminary pass handles cases with even no variables
			if(injectIntoHead(head, null, null))
				somethingChanged = true;
			
			List<String> variablesInHead = new ArrayList<String>();
			addVariableNames(variablesInHead, head);
			for(GdlLiteral literal : rule.getBody()) {
				if(!(literal instanceof GdlNot)) {
					if(injectVariable(head, literal, variablesInHead))
						somethingChanged = true;
				}
			}
			
			if(somethingChanged)
				result = true;
		}
		return result;
	}
	
	private boolean injectVariable(GdlSentence head, GdlLiteral literal,
			List<String> vars) {
		//Convert this into a sentence
		if(literal instanceof GdlSentence) {
			return injectVariableFromSentence(head, (GdlSentence)literal, vars);
		} else if(literal instanceof GdlOr) {
			boolean changed = false;
			GdlOr or = (GdlOr) literal;
			for(int i = 0; i < or.arity(); i++)
				if(injectVariable(head, or.get(i), vars))
					changed = true;
			return changed;
		} else if((literal instanceof GdlDistinct) || (literal instanceof GdlNot)) {
			//Contains no useful information
			return false;
		} else {
			throw new RuntimeException("Unforeseen literal type " + literal.getClass() + " encountered during variable injection");
		}
	}
	private boolean injectVariableFromSentence(GdlSentence head,
			GdlSentence sentence, List<String> vars) {
		//Crawl the sentence and its model together, looking
		//for where the variable refers
		String sentenceKey = sentence.getName().getValue() + "_" + sentence.arity();
		if(sentences.containsKey(sentenceKey)) {
			List<GdlTerm> body;
			try { body = sentence.getBody(); } catch(RuntimeException e) {body = Collections.emptyList();}
			List<TermModel> bodyModel = sentences.get(sentenceKey);
			//Look for the variable in the body
			return crawlBody(head, body, bodyModel, vars);
		}
		return false;
	}

	private boolean crawlBody(GdlSentence head, List<GdlTerm> body,
			List<TermModel> bodyModel, List<String> vars) {
		boolean changedSomething = false;
		//Look for the variable in the body
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlVariable) {
				if(vars.contains(term.toString())) {
					if(injectIntoHead(head, termModel, term.toString()))
						changedSomething = true;
				}
			} else if(term instanceof GdlFunction) {
				//Crawl the function, too
				GdlFunction function = (GdlFunction) term;
				String functionKey = function.getName().getValue() + "_" + function.arity();
				if(termModel.containsFunction(functionKey)) {
					//crawl function body
					List<GdlTerm> functionBody = function.getBody();
					List<TermModel> functionBodyModel = termModel.getFunction(functionKey);
					if(crawlBody(head, functionBody, functionBodyModel, vars))
						changedSomething = true;
				}
			}
		}
		return changedSomething;
	}

	private boolean injectIntoHead(GdlSentence head,
			TermModel termModel, String varName) {
		boolean changedSomething = false;
		//Since this is a head, we construct as we crawl the model,
		//rather than ignoring cases where the model is incomplete.
		//This is even true for random constants we encounter.
		String headKey = head.getName().getValue() + "_" + head.arity();
		if(!sentences.containsKey(headKey)) {
			List<TermModel> termsList = new ArrayList<TermModel>(head.arity());
			for(int i = 0; i < head.arity(); i++)
				termsList.add(new TermModel());
			sentences.put(headKey, termsList);
			changedSomething = true;
		}

		List<GdlTerm> headBody;
		try{ headBody = head.getBody(); } catch(RuntimeException e) {headBody = Collections.emptyList();}
		List<TermModel> headBodyModel = sentences.get(headKey);
		if(buildBodyModel(headBody, headBodyModel, termModel, varName))
			changedSomething = true;
		
		return changedSomething;
	}
	private boolean buildBodyModel(List<GdlTerm> body,
			List<TermModel> bodyModel, TermModel termToInject, String varName) {
		boolean changedSomething = false;
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlConstant) {
				if(!termModel.containsConstant((GdlConstant)term)) {
					termModel.addConstant((GdlConstant)term);
					changedSomething = true;
				}
			} else if(term instanceof GdlVariable) {
				if(((GdlVariable)term).getName().equals(varName)) {
					if(termModel.inject(termToInject))
						changedSomething = true;
				}
			} else if(term instanceof GdlFunction) {
				//Get the function, then the function body
				GdlFunction function = (GdlFunction) term;
				String functionKey = function.getName().getValue() + "_" + function.arity();
				if(!termModel.containsFunction(functionKey)) {
					termModel.addFunction(function.getName().getValue(), function.arity());
					changedSomething = true;
				}
				List<GdlTerm> functionBody = function.getBody();
				List<TermModel> functionBodyModel = termModel.getFunction(functionKey);
				if(buildBodyModel(functionBody, functionBodyModel, termToInject, varName))
					changedSomething = true;
			} else {
				throw new RuntimeException("Unforeseen term type " + term.getClass());
			}
		}
		return changedSomething;
	}

	//This could probably be factor outed into some utility class. 
	public static List<GdlVariable> getVariables(GdlRule rule) {
		List<String> variableNames = getVariableNames(rule);
		List<GdlVariable> variables = new ArrayList<GdlVariable>();
		for(String name : variableNames)
			variables.add(GdlPool.getVariable(name));
		return variables;
	}
	//I happened to have written the name-finding code first; the other way around would
	//probably be more efficient. The use of a list over a set is also for historical
	//reasons, though some applications could make use of the consistent ordering.
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
	private static void addVariableNames(List<String> variables, GdlLiteral literal) {
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
		} else if(literal instanceof GdlDistinct || literal instanceof GdlProposition) {
			//This should not be the only time a variable appears in a rule,
			//so we don't need to bother with it
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

	@Override
	public String toString() {
		String result = "";
		result += "{\n  ";
		
		for(Entry<String, List<TermModel>> relType : sentences.entrySet()) {
			result += "  (" + relType.getKey();
			for(TermModel m : relType.getValue()) {
				result += " " + m;
			}
			result += ")\n";
		}
		result += "}\n";
		
		return result;
	}
	
	private void addToModel(GdlRelation relation) {
		//Add a nice, simple relation with no symbols to the model
		String name = relation.getName().getValue();
		int arity = relation.arity();
		updateTermsListOfName(name, arity, relation);
	}
	
	private void updateTermsListOfName(String name, int arity, GdlRelation relation) {
		if(!sentences.containsKey(name + "_" + arity)) {
			List<TermModel> termsList = new ArrayList<TermModel>(arity);
			for(int i = 0; i < arity; i++)
				termsList.add(new TermModel());
			sentences.put(name + "_" + arity, termsList);
		}
		List<TermModel> terms = sentences.get(name + "_" + arity); 
		//For each slot in the body model, add the current term
		for(int i = 0; i < arity; i++) {
			terms.get(i).addTerm(relation.get(i));
		}	
	}


	public static class TermModel {
		Set<GdlConstant> constantValues = new HashSet<GdlConstant>();
		Map<String, List<TermModel>> functionalValues = new HashMap<String, List<TermModel>>();
		public TermModel () {
		}
		
		public TermModel(TermModel other) {
			//Copy other term model without sharing any memory
			constantValues.addAll(other.constantValues);
			for(String key : other.functionalValues.keySet()) {
				functionalValues.put(key, copy(other.functionalValues.get(key)));
			}
		}

		public void addTerm(GdlTerm term) {
			if(term instanceof GdlConstant) {
				constantValues.add((GdlConstant) term);
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				//Add the function
				//Do we already have a function of this name and arity?
				int arity = function.arity();
				String key = function.getName().getValue() + "_" + arity;
				//If not, add it to the list
				if(!functionalValues.containsKey(key)) {
					List<TermModel> terms = new ArrayList<TermModel>(arity);
					for(int i = 0; i < arity; i++)
						terms.add(new TermModel());
					functionalValues.put(key, terms);
				}
				List<TermModel> terms = functionalValues.get(key);
				//Add to each term model its value
				for(int i = 0; i < arity; i++) {
					terms.get(i).addTerm(function.get(i));
				}
			} else {
				throw new RuntimeException("Description contains non-constant, non-function term " + term + " in a supposedly constant relation");
			}
		}
		

		public boolean inject(TermModel other) {
			//Return true if we add anything new
			boolean changedSomething = false;
			
			if(constantValues.addAll(other.constantValues))
				changedSomething = true;
			for(Entry<String, List<TermModel>> function : other.functionalValues.entrySet()) {
				//If we don't have the function, add it and a copy of its contents
				String functionKey = function.getKey();
				List<TermModel> functionBody = function.getValue();
				if(!functionalValues.containsKey(functionKey)) {
					functionalValues.put(functionKey, copy(functionBody));
					changedSomething = true;
				} else {
					List<TermModel> ourFunction = functionalValues.get(functionKey);
					//Inject all the inner terms?
					for(int i = 0; i < ourFunction.size(); i++) {
						if(ourFunction.get(i).inject(functionBody.get(i)))
							changedSomething = true;
					}
				}
			}
			
			return changedSomething;
		}


		public boolean containsConstant(GdlConstant constant) {
			return constantValues.contains(constant);
		}
		public void addConstant(GdlConstant constant) {
			constantValues.add(constant);
		}

		public boolean containsFunction(String functionKey) {
			return functionalValues.containsKey(functionKey);
		}
		public List<TermModel> getFunction(String functionKey) {
			return functionalValues.get(functionKey);
		}
		public void addFunction(String functionName, int arity) {
			String functionKey = functionName + "_" + arity;
			if(functionalValues.containsKey(functionKey))
				throw new RuntimeException("Trying to add already-existing function key " + functionKey);
			List<TermModel> termsList = new ArrayList<TermModel>(arity);
			for(int i = 0; i < arity; i++)
				termsList.add(new TermModel());
			functionalValues.put(functionKey, termsList);
		}



		@Override
		public String toString() {
			String result = "{";
			for(GdlConstant c : constantValues) {
				result += c.getValue() + ", ";
			}
			for(Entry<String, List<TermModel>> fn : functionalValues.entrySet()) {
				result += "(" + fn.getKey();
				for(TermModel term : fn.getValue())
					result += " " + term;
				result += "), ";
			}
			result += "}";
			return result;
		}

		public boolean hasFunctions() {
			return !functionalValues.isEmpty();
		}

		public List<TermModel> getFunction(GdlFunction function) {
			return getFunction(function.getName().getValue() + "_" + function.arity());
		}

		public Set<GdlConstant> getConstants() {
			return constantValues;
		}

		public Map<String, List<TermModel>> getFunctions() {
			return functionalValues;
		}

		public static TermModel getIntersection(Collection<TermModel> list) {
			if(list.isEmpty())
				throw new RuntimeException("Looking for the intersection of an empty set of TermModels");
			
			Iterator<TermModel> itr = list.iterator();
			TermModel intersection = new TermModel(itr.next());
			
			while(itr.hasNext()) {
				TermModel cur = itr.next();
				//Intersect cur into the current intersection
				intersection.intersect(cur);
			}
			return intersection;
		}

		private void intersect(TermModel other) {
			//Keep only the constants that are in both.
			//Keep a function only if it appears in both;
			//intersect each of the term models in the models
			constantValues.retainAll(other.constantValues);
			Set<String> keys = new HashSet<String>(functionalValues.keySet());
			for(String key : keys) {
				if(!other.containsFunction(key)) {
					functionalValues.remove(key);
				} else {
					//Intersect each term of the function's body with the other function
					List<TermModel> ourBody = functionalValues.get(key);
					List<TermModel> theirBody = other.functionalValues.get(key);
					for(int i = 0; i < ourBody.size(); i++) {
						ourBody.get(i).intersect(theirBody.get(i));
					}
				}
			}
		}
	}


	public static List<TermModel> copy(List<TermModel> body) {
		List<TermModel> copy = new ArrayList<TermModel>();
		for(TermModel t : body) {
			copy.add(new TermModel(t));
		}
		return copy;
	}

	public List<TermModel> getBodyForSentence(GdlSentence sentence) {
		//This is some relation...
		//We just look at its name and arity
		String key = sentence.getName().getValue() + "_" + sentence.arity();
		return sentences.get(key);
	}



}
