package util.gdl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
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
	List<Gdl> description;
	private Map<String, List<TermModel>> sentences = new HashMap<String, List<TermModel>>();
	int maxArity = 0;
	boolean ignoreLanguageRules;
	
	public SentenceModel(List<Gdl> description, boolean ignoreLanguageRules) {
		this.description = description;
		this.ignoreLanguageRules = ignoreLanguageRules;
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
		if(!ignoreLanguageRules) {
			if(!sentences.containsKey("legal")) {
				List<TermModel> termsList = new ArrayList<TermModel>(2);
				termsList.add(new TermModel());
				termsList.add(new TermModel());
				sentences.put("legal", termsList);
			}
			if(!sentences.containsKey("does")) {
				List<TermModel> termsList = new ArrayList<TermModel>(2);
				termsList.add(new TermModel());
				termsList.add(new TermModel());
				sentences.put("does", termsList);
			}
			if(!sentences.containsKey("init")) {
				List<TermModel> termsList = new ArrayList<TermModel>(1);
				termsList.add(new TermModel());
				sentences.put("init", termsList);
			}
			if(!sentences.containsKey("next")) {
				List<TermModel> termsList = new ArrayList<TermModel>(1);
				termsList.add(new TermModel());
				sentences.put("next", termsList);
			}
			if(!sentences.containsKey("true")) {
				List<TermModel> termsList = new ArrayList<TermModel>(1);
				termsList.add(new TermModel());
				sentences.put("true", termsList);
			}
		}
		
		//Now, apply injections through rules until we've gotten absolutely everything
		//The brute-force method is to repeatedly apply injection at every rule until nothing new is added
		//GDL descriptions are generally small enough that this should work
		boolean somethingChanged = true;
		while(somethingChanged) {
			somethingChanged = false;
			if(!ignoreLanguageRules && applyLanguageInjections())
				somethingChanged = true;
			for(GdlRule rule : rules) {
				//We apply the injection, and note if it changes the model
				if(applyInjection(rule))
					somethingChanged = true;
			}
		}
		
		for(List<TermModel> body : sentences.values())
			if(maxArity < body.size())
				maxArity = body.size();
	}
	
	public SentenceModel(List<Gdl> description) {
		this(description, false);
	}

	private boolean applyLanguageInjections() {
		//Injects init and next to true, and legal to does.
		boolean somethingChanged = false;
		
		List<TermModel> legalBody, doesBody, initBody, nextBody, trueBody;
		legalBody = sentences.get("legal");
		doesBody = sentences.get("does");
		initBody = sentences.get("init");
		nextBody = sentences.get("next");
		trueBody = sentences.get("true");
		
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
		String sentenceName = sentence.getName().getValue();
		if(sentences.containsKey(sentenceName)) {
			List<GdlTerm> body;
			try { body = sentence.getBody(); } catch(RuntimeException e) {body = Collections.emptyList();}
			List<TermModel> bodyModel = sentences.get(sentenceName);
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
				String functionName = function.getName().getValue();
				if(termModel.containsFunction(functionName)) {
					//crawl function body
					List<GdlTerm> functionBody = function.getBody();
					List<TermModel> functionBodyModel = termModel.getFunction(functionName);
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
		String headKey = head.getName().getValue();
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
				String functionName = function.getName().getValue();
				if(!termModel.containsFunction(functionName)) {
					termModel.addFunction(function.getName().getValue(), function.arity());
					changedSomething = true;
				}
				List<GdlTerm> functionBody = function.getBody();
				List<TermModel> functionBodyModel = termModel.getFunction(functionName);
				if(buildBodyModel(functionBody, functionBodyModel, termToInject, varName))
					changedSomething = true;
			} else {
				throw new RuntimeException("Unforeseen term type " + term.getClass());
			}
		}
		return changedSomething;
	}
	
	public void restrictDomainsToUsefulValues() {
		//We want to restrict the domains of sentence forms to those that will be
		//useful looking forward; that is, they must eventually be passed into
		//some statement with a next/legal/goal/init __, or else be __, or
		//used in a not or distinct statement. Also, any domain that ends up
		//being reduced should be maintained.
		//The motivator for this is the set of chinese checkers games, and
		//specifically the "count" sentence.
		
		//Basically, for non-special sentences, the domain is the union of what
		//we find from the following, as parts of rule bodies:
		//1) If it's part of a variable with no equivalent in the
		//   head, keep all of the domain.
		//2) If it's a constant in the sentence (as used in the domain), keep
		//   that constant.
		//I guess if it's any
		
		//So, we should have a new domain for each term model, I guess...
		Map<TermModel, Set<GdlConstant>> newDomains = new HashMap<TermModel, Set<GdlConstant>>();
		//We go through each rule body
		//Don't forget the special ones...
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				for(GdlLiteral literal : rule.getBody()) {
					addToRestrictedDomains(literal, newDomains);
				}
			}
		}
		
		//Now, we restrict all the domains we found
		for(Entry<TermModel, Set<GdlConstant>> entry : newDomains.entrySet()) {
			entry.getKey().setConstants(entry.getValue());
		}
	}

	private void addToRestrictedDomains(GdlLiteral literal,
			Map<TermModel, Set<GdlConstant>> newDomains) {
		if(literal instanceof GdlRelation) {
			GdlRelation relation = (GdlRelation) literal;
			//There are certain domains we don't want to restrict.
			String relationName = relation.getName().getValue();
			if(relationName.equals("does")
					|| relationName.equals("legal")
					|| relationName.equals("goal")
					|| relationName.equals("init")
					|| relationName.equals("true")
					|| relationName.equals("next")
					|| relationName.equals("base")
					|| relationName.equals("input"))
				return;
			//Get the list of terms and the list of term models
			List<GdlTerm> body = relation.getBody();
			List<TermModel> bodyModel = getBody(relation.getName().getValue());
			addToRestrictedDomains(body, bodyModel, newDomains);
		} else if(literal instanceof GdlNot) {
			GdlNot not = (GdlNot) literal;
			addToRestrictedDomains(not.getBody(), newDomains);
		} else if(literal instanceof GdlOr) {
			GdlOr or = (GdlOr) literal;
			for(int i = 0; i < or.arity(); i++) {
				addToRestrictedDomains(or.get(i), newDomains);
			}
		}
	}

	private void addToRestrictedDomains(List<GdlTerm> body,
			List<TermModel> bodyModel,
			Map<TermModel, Set<GdlConstant>> newDomains) {
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlConstant) {
				if(!newDomains.containsKey(termModel))
					newDomains.put(termModel, new HashSet<GdlConstant>());
				newDomains.get(termModel).add((GdlConstant)term);
			} else if(term instanceof GdlVariable) {
				//Can't really rule anything out of the domain
				newDomains.put(termModel, termModel.getConstants());
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<GdlTerm> functionBody = function.getBody();
				List<TermModel> functionBodyModel = termModel.getFunction(function);
				addToRestrictedDomains(functionBody, functionBodyModel, newDomains);
			}
		}
	}

	//This could probably be factored out into some utility class. 
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
		if(!sentences.containsKey(name)) {
			List<TermModel> termsList = new ArrayList<TermModel>(arity);
			for(int i = 0; i < arity; i++)
				termsList.add(new TermModel());
			sentences.put(name, termsList);
		}
		List<TermModel> terms = sentences.get(name);
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
		
		void setConstants(Set<GdlConstant> constants) {
			this.constantValues = constants;
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
				String name = function.getName().getValue();
				//If not, add it to the list
				if(!functionalValues.containsKey(name)) {
					List<TermModel> terms = new ArrayList<TermModel>(arity);
					for(int i = 0; i < arity; i++)
						terms.add(new TermModel());
					functionalValues.put(name, terms);
				}
				List<TermModel> terms = functionalValues.get(name);
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
			if(functionalValues.containsKey(functionName))
				throw new RuntimeException("Trying to add already-existing function name " + functionName);
			List<TermModel> termsList = new ArrayList<TermModel>(arity);
			for(int i = 0; i < arity; i++)
				termsList.add(new TermModel());
			functionalValues.put(functionName, termsList);
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
			return getFunction(function.getName().getValue());
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
		//We just look at its name
		return sentences.get(sentence.getName().getValue());
	}

	/**
	 * Returns the set of all constants used in the game. Does not include
	 * relation constants (i.e. constants used only as the names of domains).
	 */
	public Set<GdlConstant> getUniversalDomain() {
		Set<GdlConstant> domain = new HashSet<GdlConstant>();
		for(List<TermModel> body : sentences.values()) {
			addConstantsToDomain(body, domain);
		}
		return domain;
	}
	private void addConstantsToDomain(List<TermModel> body, Set<GdlConstant> domain) {
		for(TermModel term : body) {
			domain.addAll(term.getConstants());
			for(List<TermModel> functionBody : term.getFunctions().values()) {
				addConstantsToDomain(functionBody, domain);
			}
		}
	}

	Map<SentenceForm, Set<SentenceForm>> dependencyGraph = null;
	/**
	 * Each key in the graph depends on those sentence forms in the associated set.
	 */
	public Map<SentenceForm, Set<SentenceForm>> getDependencyGraph() {
		if(dependencyGraph == null) {
			//Build graph from rules
			dependencyGraph = new HashMap<SentenceForm, Set<SentenceForm>>();
			for(Gdl gdl : description) {
				if(gdl instanceof GdlRule) {
					GdlRule rule = (GdlRule) gdl;
					SentenceForm head = new SentenceForm(rule.getHead());
					if(!dependencyGraph.containsKey(head))
						dependencyGraph.put(head, new HashSet<SentenceForm>());
					for(GdlLiteral bodyLiteral : rule.getBody()) {
						dependencyGraph.get(head).addAll(extractSentenceForms(bodyLiteral));
					}
				}
			}
		}
		return dependencyGraph;
	}
	
	/**
	 * Independent sentence forms are those whose sentences do
	 * not depend on players' actions; they may depend on which
	 * turn it is, however. For example, a sentence form (true (step _))
	 * implementing a step counter would be independent, but
	 * not constant. All constant sentence forms are independent.
	 */
	public Set<SentenceForm> getIndependentSentenceForms() {
		if(independentSentenceForms == null)
			setDependentAndIndependentSentenceForms();
		return independentSentenceForms;
	}
	private Set<SentenceForm> independentSentenceForms = null;
	/**
	 * Constant sentence forms are those whose sentences are
	 * either true in all states or false in all states. 
	 */
	public Set<SentenceForm> getConstantSentenceForms() {
		if(constantSentenceForms == null) {
			constantSentenceForms = new HashSet<SentenceForm>();
			constantSentenceForms.addAll(getSentenceForms());
			constantSentenceForms.removeAll(getChangingSentences(true));
			
			constantSentenceForms = Collections.unmodifiableSet(constantSentenceForms);
		}
		return constantSentenceForms;
	}
	private Set<SentenceForm> constantSentenceForms = null;
	/**
	 * Dependent sentence forms are those with at least some
	 * sentences whose truth values can vary based on the players'
	 * choices of actions.
	 */
	public Set<SentenceForm> getDependentSentenceForms() {
		if(dependentSentenceForms == null)
			setDependentAndIndependentSentenceForms();
		return dependentSentenceForms;
	}
	private Set<SentenceForm> dependentSentenceForms = null;
	private void setDependentAndIndependentSentenceForms() {
		//We'll need to go over the graph...
		dependentSentenceForms = getChangingSentences(false);
		independentSentenceForms = new HashSet<SentenceForm>();
		independentSentenceForms.addAll(getSentenceForms());
		independentSentenceForms.removeAll(dependentSentenceForms);
		
		dependentSentenceForms = Collections.unmodifiableSet(dependentSentenceForms);
		independentSentenceForms = Collections.unmodifiableSet(independentSentenceForms);
	}
	
	private Set<SentenceForm> getChangingSentences(boolean includeIndependents) {
		Set<SentenceForm> allForms = getSentenceForms();
		Map<SentenceForm, Set<SentenceForm>> dependencies = getDependencyGraph();		
		Queue<SentenceForm> unfollowed = new LinkedList<SentenceForm>();
		Set<SentenceForm> changing = new HashSet<SentenceForm>();
		for(SentenceForm form : allForms) {
			if(form.getName().getValue().equals("does")) {
				unfollowed.add(form);
				changing.add(form);
			}
			//Do this next part if we want to include things that change
			//over time, but always function the same way (e.g. step counters)
			if(includeIndependents) {
				if(form.getName().getValue().equals("true")) {
					unfollowed.add(form);
					changing.add(form);
				}
			}
		}
		//Now, we propagate the unfollowed set until we have everything
		//that follows from a does clause
		//We must be careful when we encounter a "next" sentence form
		while(!unfollowed.isEmpty()) {
			SentenceForm toFollow = unfollowed.remove();
			for(SentenceForm candidate : allForms) {
				if(changing.contains(candidate))
					continue;
				if(dependencies.get(candidate) != null
						&& dependencies.get(candidate).contains(toFollow)) {
					//The candidate is dependent on a changing form
					changing.add(candidate);
					unfollowed.add(candidate);
					if(candidate.getName().getValue().equals("next")) {
						SentenceForm trueForm = candidate.getCopyWithName("true");
						changing.add(trueForm);
						unfollowed.add(trueForm);
					}
				}
			}
		}
		return changing;
	}


	private Set<SentenceForm> extractSentenceForms(GdlLiteral literal) {
		Set<SentenceForm> forms = new HashSet<SentenceForm>();
		extractSentenceForms(forms, literal);
		return forms;
	}
	private void extractSentenceForms(Collection<SentenceForm> forms, GdlLiteral literal) {
		if(literal instanceof GdlSentence) {
			forms.add(new SentenceForm((GdlSentence)literal));
		} else if(literal instanceof GdlNot) {
			extractSentenceForms(forms, ((GdlNot) literal).getBody());
		} else if(literal instanceof GdlOr) {
			GdlOr or = (GdlOr) literal;
			for(int i = 0; i < or.arity(); i++)
				extractSentenceForms(forms, or.get(i));
		}
		//Distincts are unnecessary to record
	}


	public class SentenceForm implements Iterable<GdlSentence> {
		public SentenceForm(GdlSentence sentence) {
			sentenceName = sentence.getName();
			tupleSize = 0; //Gets filled in as we go
			if(sentence instanceof GdlProposition)
				return;
			GdlRelation relation = (GdlRelation) sentence;
			fillElementsWithBody(relation.getBody(), 0);
		}
		public SentenceForm(SentenceForm original, String name) {
			//Construct a SentenceForm based on another, but with a different relation name
			sentenceName = GdlPool.getConstant(name);
			//These are immutable, so we can just reference the originals
			functionalElements = original.functionalElements;
			arities = original.arities;
			tupleSize = original.tupleSize;
		}
		private int fillElementsWithBody(List<GdlTerm> body, int termNumber) {
			for(GdlTerm term : body) {
				if(term instanceof GdlConstant || term instanceof GdlVariable) {
					functionalElements.add(null);
					arities.add(0);
					termNumber++;
					tupleSize++;
				} else if(term instanceof GdlFunction) {
					GdlFunction function = (GdlFunction) term;
					functionalElements.add(function.getName());
					arities.add(function.arity());
					termNumber++;
					termNumber = fillElementsWithBody(function.getBody(), termNumber); 
				}
			}
			return termNumber;
		}
		//The sentence form is a selection of a relation constant and any
		//functions it contains. That is, two relations with the same
		//sentence form may differ only in their constants and variables.
		private GdlConstant sentenceName;
		private List<GdlConstant> functionalElements = new ArrayList<GdlConstant>();
		private List<Integer> arities = new ArrayList<Integer>();
		
		private int tupleSize = 0;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			//result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((arities == null) ? 0 : arities.hashCode());
			result = prime
					* result
					+ ((functionalElements == null) ? 0 : functionalElements
							.hashCode());
			result = prime * result
					+ ((sentenceName == null) ? 0 : sentenceName.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SentenceForm other = (SentenceForm) obj;
			//if (!getOuterType().equals(other.getOuterType()))
			//	return false;
			if (arities == null) {
				if (other.arities != null)
					return false;
			} else if (!arities.equals(other.arities))
				return false;
			if (functionalElements == null) {
				if (other.functionalElements != null)
					return false;
			} else if (!functionalElements.equals(other.functionalElements))
				return false;
			if (sentenceName == null) {
				if (other.sentenceName != null)
					return false;
			} else if (!sentenceName.equals(other.sentenceName))
				return false;
			return true;
		}
		//private SentenceModel getOuterType() {
		//	return SentenceModel.this;
		//}
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("(").append(sentenceName);
			Stack<Integer> arityStack = new Stack<Integer>();
			for(int i = 0; i < functionalElements.size(); i++) {
				GdlConstant functionName = functionalElements.get(i);
				int arity = arities.get(i);
				if(functionName == null) {
					b.append(" _");
					if(!arityStack.isEmpty()) {
						int curArity = arityStack.pop();
						if(curArity == 1)
							b.append(")");
						else
							arityStack.push(curArity - 1);
					}
				} else { //functionName != null
					b.append(" (").append(functionName);
					arityStack.push(arity);
				}
			}
			b.append(")");
			return b.toString();
		}
		
		public GdlConstant getName() {
			return sentenceName;
		}
		public SentenceForm getCopyWithName(String name) {
			return new SentenceForm(this, name);
		}
		public boolean matches(GdlSentence sentence) {
			//Does the sentence fit the form? Hmm...
			if(sentence.getName() != sentenceName)
				return false;
			if(sentence instanceof GdlProposition)
				return functionalElements.isEmpty();
			GdlRelation relation = (GdlRelation) sentence;
			List<Integer> index = new ArrayList<Integer>(1);
			index.add(0);
			return elementsMatchBody(relation.getBody(), index);
		}
		private boolean elementsMatchBody(List<GdlTerm> body, List<Integer> index) {
			//Go through the body, increase the index as we go
			for(GdlTerm term : body) {
				if(term instanceof GdlFunction) {
					GdlFunction function = (GdlFunction) term;
					if(functionalElements.get(index.get(0)) != function.getName()) {
						return false;
					}
					if(arities.get(index.get(0)) != function.arity()) {
						return false;
					}
					//Check the function body
					index.set(0, 1 + index.get(0));
					if(!elementsMatchBody(function.getBody(), index)) {
						return false;
					}
				} else {
					if(functionalElements.get(index.get(0)) != null)
						return false;
				}
				index.set(0, 1 + index.get(0));
			}
			return true;
		}
		@Override
		public Iterator<GdlSentence> iterator() {
			if(functionalElements.isEmpty())
				return new SentenceFormPropositionIterator();
			else
				return new SentenceFormRelationIterator();
		}
		private class SentenceFormPropositionIterator implements Iterator<GdlSentence> {
			boolean used = false;
			@Override
			public boolean hasNext() {
				return !used;
			}
			@Override
			public GdlSentence next() {
				used = true;
				return GdlPool.getProposition(sentenceName);
			}
			@Override
			public void remove() {
				//Not implemented
			}
		}
		private class SentenceFormRelationIterator implements Iterator<GdlSentence> {
			List<Set<GdlConstant>> bodyDomains = new ArrayList<Set<GdlConstant>>(functionalElements.size());
			List<Iterator<GdlConstant>> bodyIterators = new ArrayList<Iterator<GdlConstant>>(functionalElements.size());
			List<GdlConstant> currentTuple = new ArrayList<GdlConstant>(functionalElements.size());
			int initIndex = 0; //used only in initialization
			public SentenceFormRelationIterator() {
				//bodyIterators: initialize
				//need to go over the domains we've established
				List<TermModel> models = sentences.get(sentenceName.getValue());
				addDomainsOfModels(models);
				//Now, set the iterators according to the domains
				for(Set<GdlConstant> domain : bodyDomains) {
					bodyIterators.add(domain.iterator());
				}
				//We wait for the current tuple...
			}
			private void addDomainsOfModels(List<TermModel> models) {
				for(TermModel model : models) {
					GdlConstant functionName = functionalElements.get(initIndex);
					if(functionName == null) {
						bodyDomains.add(model.getConstants());
						initIndex++;
					} else {
						//Recurse over the function body
						List<TermModel> functionBody = model.getFunction(functionName.getValue());
						initIndex++;
						addDomainsOfModels(functionBody);
					}
				}
			}
			@Override
			public boolean hasNext() {
				for(Iterator<GdlConstant> itr : bodyIterators)
					if(itr.hasNext())
						return true;
				return false;
			}
			@Override
			public GdlSentence next() {
				if(currentTuple.isEmpty()) {
					//Fill it for the first time
					for(Iterator<GdlConstant> itr : bodyIterators)
						currentTuple.add(itr.next());
					return getSentenceFromTuple(currentTuple);
				}
				//Starting at the end, find the first non-expired slot
				int i;
				for(i = bodyIterators.size() - 1; i >= 0; i--) {
					if(bodyIterators.get(i).hasNext())
						break;
				}
				//Update the constant at i
				currentTuple.set(i, bodyIterators.get(i).next());
				//Replace all the iterators after i
				for(int j = i + 1; j < bodyIterators.size(); j++) {
					bodyIterators.set(j, bodyDomains.get(j).iterator());
					currentTuple.set(j, bodyIterators.get(j).next());
				}
				//Now the current tuple is updated
				return getSentenceFromTuple(currentTuple);
			}
			@Override
			public void remove() {
				//Not applicable
			}
		}
		public GdlSentence getSentenceFromTuple(List<GdlConstant> tuple) {
			/*if(tuple.size() != functionalElements.size())
				throw new RuntimeException("Tried to get sentence from tuple of size "+tuple.size()
						+"; need size " + functionalElements.size());*/
			if(tuple.size() == 0)
				return GdlPool.getProposition(sentenceName);
			//Make the GdlRelation corresponding to this as a tuple
			List<Integer> index = new ArrayList<Integer>(2);
			index.add(0);
			index.add(0);
			List<GdlTerm> body = new ArrayList<GdlTerm>();
			//we could bootstrap the arity...
			int arity = arities.size();
			for(int a : arities)
				arity -= a;
			fillBody(body, index, tuple, arity);
			return GdlPool.getRelation(sentenceName, body);
		}
		private void fillBody(List<GdlTerm> body, List<Integer> index,
				List<GdlConstant> tuple, int arity) {
			for(int i = 0; i < arity; i++) {
				//fill in the ith element of the body
				//starting with the index1th element of functionalElements
				//constants filled by the index2th element of tuple
				GdlConstant functionName = functionalElements.get(index.get(0));
				if(functionName == null) {
					body.add(tuple.get(index.get(1)));
					index.set(0, 1 + index.get(0));
					index.set(1, 1 + index.get(1));
				} else {
					//add the function
					int a = arities.get(index.get(0));
					List<GdlTerm> functionBody = new ArrayList<GdlTerm>(a);
					index.set(0, 1 + index.get(0));
					fillBody(functionBody, index, tuple, a);
					body.add(GdlPool.getFunction(functionName, functionBody));
				}
			}
		}
		public int getTupleSize() {
			return tupleSize;
		}

		private GdlSentence sampleSentence = null;
		public GdlSentence getSampleSentence() {
			if(sampleSentence == null) {
				//TODO: Fill in values from actual domain?
				List<GdlConstant> tuple = new ArrayList<GdlConstant>(tupleSize);
				for(int i = 0; i < tupleSize; i++)
					tuple.add(GdlPool.getConstant("0"));
				sampleSentence = getSentenceFromTuple(tuple);
			}
			return sampleSentence;
		}
		
	}
	
	public static boolean inSentenceFormGroup(GdlSentence sentence,
			Set<SentenceForm> forms) {
		for(SentenceForm form : forms)
			if(form.matches(sentence))
				return true;
		return false;
	}

	public static List<GdlConstant> getTupleFromGroundSentence(
			GdlSentence sentence) {
		if(sentence instanceof GdlProposition)
			return Collections.emptyList();
		
		//A simple crawl through the sentence.
		List<GdlConstant> tuple = new ArrayList<GdlConstant>();
		try {
			addBodyToTuple(sentence.getBody(), tuple);
		} catch(RuntimeException e) {
			throw new RuntimeException(e.getMessage() + "\nSentence was " + sentence);
		}
		return tuple;
	}
	private static void addBodyToTuple(List<GdlTerm> body, List<GdlConstant> tuple) {
		for(GdlTerm term : body) {
			if(term instanceof GdlConstant) {
				tuple.add((GdlConstant) term);
			} else if(term instanceof GdlVariable) {
				throw new RuntimeException("Asking for a ground tuple of a non-ground sentence");
			} else if(term instanceof GdlFunction){
				GdlFunction function = (GdlFunction) term;
				addBodyToTuple(function.getBody(), tuple);
			} else {
				throw new RuntimeException("Unforeseen Gdl tupe in SentenceModel.addBodyToTuple()");
			}
		}
	}

	public Set<String> getSentenceNames() {
		//return sentences.keySet();
		Set<String> sentenceNames = new HashSet<String>();
		for(String taggedName : sentences.keySet()) {
			sentenceNames.add(taggedName);
		}
		return sentenceNames;
	}

	public List<TermModel> getBody(String relationName) {
		return sentences.get(relationName);
	}

	public void addSentence(String newName, String sentenceToCopy) {
		//System.out.println(newName + ", " + sentenceToCopy);
		//System.out.println("stc: " + sentences.get(sentenceToCopy));
		if(!sentences.containsKey(newName))
			sentences.put(newName, copy(sentences.get(sentenceToCopy)));
		else
			injectIntoSameSentenceForm(sentences.get(newName), sentences.get(sentenceToCopy));
	}

	private void injectIntoSameSentenceForm(List<TermModel> newBody,
			List<TermModel> oldBody) {
		if(newBody.size() != oldBody.size())
			throw new RuntimeException();
		//A one-to-one injection
		for(int i = 0; i < oldBody.size(); i++) {
			newBody.get(i).inject(oldBody.get(i));
		}
	}

	private Set<SentenceForm> sentenceForms = null;
	public Set<SentenceForm> getSentenceForms() {
		if(sentenceForms == null) {
			sentenceForms = new HashSet<SentenceForm>();
			for(Gdl gdl : description) {
				if(gdl instanceof GdlRelation) {
					extractSentenceForms(sentenceForms, (GdlRelation) gdl);
				} else if(gdl instanceof GdlRule) {
					GdlRule rule = (GdlRule) gdl;
					extractSentenceForms(sentenceForms, rule.getHead());
					for(GdlLiteral literal : rule.getBody())
						extractSentenceForms(sentenceForms, literal);
				}
			}
			if(!ignoreLanguageRules)
				addImpliedSentenceForms(sentenceForms);
			sentenceForms = Collections.unmodifiableSet(sentenceForms);
		}
		return sentenceForms;
	}

	private void addImpliedSentenceForms(Set<SentenceForm> forms) {
		Set<SentenceForm> formsToAdd = new HashSet<SentenceForm>();
		for(SentenceForm form : forms) {
			if(form.getName().getValue().equals("next"))
				formsToAdd.add(form.getCopyWithName("true"));
			else if(form.getName().getValue().equals("init"))
				formsToAdd.add(form.getCopyWithName("true"));
			else if(form.getName().getValue().equals("legal"))
				formsToAdd.add(form.getCopyWithName("does"));
		}
		forms.addAll(formsToAdd);
	}

	Map<SentenceForm, Set<GdlRelation>> relationsByForm = new HashMap<SentenceForm, Set<GdlRelation>>();
	public Set<GdlRelation> getRelations(SentenceForm form) {
		if(relationsByForm.get(form) == null) {
			Set<GdlRelation> relations = new HashSet<GdlRelation>();
			
			//build it...
			for(Gdl gdl : description) {
				if(gdl instanceof GdlRelation) {
					GdlRelation relation = (GdlRelation) gdl;
					if(form.matches(relation))
						relations.add(relation);
				}
			}
			
			relationsByForm.put(form, relations);
		}
		return relationsByForm.get(form);
	}

	Map<SentenceForm, Set<GdlRule>> rulesByForm = new HashMap<SentenceForm, Set<GdlRule>>();
	/**
	 * Returns the rules that GENERATE the sentence form, not necessarily
	 * all the rules that contain it.
	 * 
	 * Note that if functions can be assigned to variables, this might not
	 * find all the rules capable of generating sentences of the given form.
	 */
	public Set<GdlRule> getRules(SentenceForm form) {
		if(rulesByForm.get(form) == null) {
			Set<GdlRule> rules = new HashSet<GdlRule>();
			
			for(Gdl gdl : description) {
				if(gdl instanceof GdlRule) {
					GdlRule rule = (GdlRule) gdl;
					if(form.matches(rule.getHead()))
						rules.add(rule);
				}
			}
			
			rulesByForm.put(form, rules);
		}
		return rulesByForm.get(form);
	}

	public SentenceForm getSentenceForm(GdlSentence sentence) {
		Set<SentenceForm> forms = getSentenceForms();
		for(SentenceForm form : forms)
			if(form.matches(sentence))
				return form;
		return null;
	}


}
