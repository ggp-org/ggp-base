package org.ggp.base.util.gdl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.GdlVisitor;
import org.ggp.base.util.gdl.GdlVisitors;
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
import org.ggp.base.util.gdl.transforms.ConstantFinder.ConstantChecker;


/**
 * A model of all the sentences that can be formed (loosely speaking) in a particular game.
 * For each relation and function, this model contains the values (both constants and functions)
 * that each of its terms may have, according to some simple analysis of the rules. The values
 * found are a superset of the actual values that will be found in the game; not all of these
 * values are necessarily reachable in the actual game.
 *
 * @author Alex Landau
 */
public class SentenceModelImpl implements RuleSplittableSentenceModel {
	private final List<Gdl> description;
	private final Map<String, List<TermModel>> sentences = new HashMap<String, List<TermModel>>();
	private final boolean ignoreLanguageRules;

	public SentenceModelImpl(List<Gdl> description, boolean ignoreLanguageRules) throws InterruptedException {
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
				ConcurrencyUtils.checkForInterruption();
			}
		}

	}

	public SentenceModelImpl(List<Gdl> description) throws InterruptedException {
		this(description, false);
	}

	/**
	 * Constructs a deep copy of the given model.
	 */
	public SentenceModelImpl(SentenceModelImpl other) {
		//Part of the reason this exists is to allow malleable descriptions.
		//For most objects, this will mean new data structures sharing the
		//same immutable objects.
		description = new ArrayList<Gdl>(other.description);
		//TermModels are not immutable.
		for(Entry<String, List<TermModel>> entry : other.sentences.entrySet())
			sentences.put(entry.getKey(), TermModel.copy(entry.getValue()));
		//maxArity = other.maxArity;
		ignoreLanguageRules = other.ignoreLanguageRules;

		if(other.dependencyGraph != null)
			dependencyGraph = new HashMap<SentenceForm, Set<SentenceForm>>(other.dependencyGraph);
		//SentenceForms are immutable.
		if(other.constantSentenceForms != null)
			constantSentenceForms = new HashSet<SentenceForm>(other.constantSentenceForms);
		if(other.independentSentenceForms != null)
			independentSentenceForms = new HashSet<SentenceForm>(other.independentSentenceForms);
		if(other.dependentSentenceForms != null)
			dependentSentenceForms = new HashSet<SentenceForm>(other.dependentSentenceForms);

		if(other.sentenceForms != null)
			sentenceForms = new HashSet<SentenceForm>(other.sentenceForms);
		if(other.sentenceFormsByName != null)
			sentenceFormsByName = new HashMap<GdlConstant, Set<SentenceForm>>(other.sentenceFormsByName);
		relationsByForm.putAll(other.relationsByForm);
		rulesByForm.putAll(other.rulesByForm);
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

	private boolean applyInjection(GdlRule rule) throws InterruptedException {
		boolean result = false;
		boolean somethingChanged = true;
		while(somethingChanged) {
			ConcurrencyUtils.checkForInterruption();

			somethingChanged = false;

			//For each variable in the LHS of the rule, we want
			//to get all the possible values implied by each
			//positive conjunct on the RHS.
			GdlSentence head = rule.getHead();

			//This preliminary pass handles cases with even no variables
			if(injectIntoHead(head, null, null))
				somethingChanged = true;

			List<String> variablesInHead = GdlUtils.getVariableNames(head);
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

	//Don't use this if you have function-valued variables.
	//To make this simpler, let's also say no "ors" allowed.
	@Override
	public void restrictDomainsToUsefulValues(ConstantChecker constantChecker) throws InterruptedException {
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

		//TODO: We need to expand this to handle, e.g., mummymaze1p. Here, we use
		//a different set of rules:
		//The domain is the union of what we find from the following:
		//1) If it's part of a variable with no equivalent in the head, keep
		//   the intersection of all appearances of the variable in positive
		//   conjuncts in the rule body.
		//2) If it's a constant in the sentence, keep that constant.

		//Can we get a more formal/useful definition?
		//We want to restrict term models. We currently have some superset of the
		//minimal domain for each term model; we want to cut down that size.
		//We want to go through all the rules and find SUPPORT for the inclusion
		//of each variable in each term model. (I suppose functions, too, but we'll
		//ignore those for now.) That means for each term model, we set up a set
		//of variables that have support so far.
		//Then we iterate over the functions and find the support that each function
		//provides to each of the relevant term models.
		//This gets slightly complicated in that we want to prune term models through
		//various layers, i.e. when the head's domain changes. Maybe repeated iteration?

		//I'm wondering if this could get ugly if we ignore the ways functions
		//are used in the term models. Let's say we have (legal ?player ?move)
		//deriving from something; the term models in the function called by
		//?move might get ignored, even though this grants them full support.

		//If we had function-valued variables removed, this would be easier.
		//But this is optional, so we can assume that.

		//Okay, here's the algorithm:
		//We find the set of constants "needed" by each term model.
		//This is initially populated in two ways:
		//1) If the term model is part of a next/goal/legal sentence,
		//   then it is needed. (Advanced: We can use base/input to
		//   throw out some of these values.)
		//2) If the term has a constant value in some sentence in the
		//   BODY of a rule, then it is needed.
		//We propagate need as follows:
		//If a term that is a variable in the head of a rule needs a
		//particular value, AND that variable is possible (i.e. in the
		//current domain) in every appearance of the variable in
		//positive conjuncts in the rule's body, then the value is
		//needed in every appearance of the variable in the rule
		//(positive or negative).

		//There is a second form of propagation:
		//If a variable does not appear in the head of a variable,
		//then all the values that are in the intersections of all the
		//domains from the positive conjuncts containing the variable
		//become needed.
		if(Thread.currentThread().isInterrupted())
			throw new InterruptedException("Interrupted in SentenceModel:397");
		while(true) {
			boolean changedSomething = false;
			if(applyNeedDomainRestriction(constantChecker))
				changedSomething = true;

			//Now we go through and go through injection a second time.
			//This eliminates values that we now know to be impossible
			//based on the constants we know about.
			if(applyPossibilityDomainRestriction(constantChecker))
				changedSomething = true;
			if(!changedSomething)
				break;
		}
	}

	private boolean applyPossibilityDomainRestriction(
			ConstantChecker constantChecker) throws InterruptedException {
		//Find the still-possible values for each TermModel.
		Map<TermModel, Set<GdlConstant>> possibleValues = new HashMap<TermModel, Set<GdlConstant>>();
		for(TermModel termModel : getAllTermModels())
			possibleValues.put(termModel, new HashSet<GdlConstant>());
		Map<TermModel, Set<GdlConstant>> oldValues = new HashMap<TermModel, Set<GdlConstant>>();
		while(true) {
			ConcurrencyUtils.checkForInterruption();

			//Rule-based injections
			for(Gdl gdl : description) {
				if(gdl instanceof GdlRelation) {
					GdlRelation relation = (GdlRelation) gdl;
					List<TermModel> bodyModel = getBody(relation.getName().getValue());
					addConstantsAsPossibleValues(relation.getBody(), bodyModel, possibleValues);
				} else if(gdl instanceof GdlRule) {
					GdlRule rule = (GdlRule) gdl;
					addPossibilitiesGivenByRule(rule, possibleValues, constantChecker);
				}
			}
			//Language-based injections
			findLanguageBasedPossibilities(possibleValues);
			if(possibleValues.equals(oldValues))
				break;
			for(TermModel termModel : possibleValues.keySet())
				oldValues.put(termModel, new HashSet<GdlConstant>(possibleValues.get(termModel)));
		}
		boolean changedSomething = false;
		for(TermModel termModel : getAllTermModels()) {
			if(termModel.getConstants().retainAll(possibleValues.get(termModel)))
				changedSomething = true;
		}
		return changedSomething;
	}

	private void findLanguageBasedPossibilities(
			Map<TermModel, Set<GdlConstant>> possibleValues) {
		List<TermModel> legalBody, doesBody, initBody, nextBody, trueBody;
		legalBody = sentences.get("legal");
		doesBody = sentences.get("does");
		initBody = sentences.get("init");
		nextBody = sentences.get("next");
		trueBody = sentences.get("true");

		propagatePossibilities(legalBody, doesBody, possibleValues);
		propagatePossibilities(initBody, trueBody, possibleValues);
		propagatePossibilities(nextBody, trueBody, possibleValues);
	}

	private void propagatePossibilities(List<TermModel> fromBody,
			List<TermModel> toBody,
			Map<TermModel, Set<GdlConstant>> possibleValues) {
		for(int i = 0; i < fromBody.size(); i++) {
			TermModel fromTerm = fromBody.get(i);
			TermModel toTerm = toBody.get(i);
			possibleValues.get(toTerm).addAll(possibleValues.get(fromTerm));
			for(String functionName : fromTerm.getFunctions().keySet()) {
				List<TermModel> fromFunction = fromTerm.getFunction(functionName);
				List<TermModel> toFunction = toTerm.getFunction(functionName);
				propagatePossibilities(fromFunction, toFunction, possibleValues);
			}
		}
	}

	private void addPossibilitiesGivenByRule(GdlRule rule,
			Map<TermModel, Set<GdlConstant>> possibleValues,
			ConstantChecker constantChecker) {
		//We add to the possibilities for terms in the head by:
		//1) Adding any constants we find in the head.
		//2) Adding the domains of any variables we find, with the domain
		//   defined across the rule and taking advantage of what we know
		//   about the constants.
		GdlSentence ruleHead = rule.getHead();
		if(ruleHead instanceof GdlProposition)
			return;
		Map<GdlVariable, Set<GdlConstant>> varDomains = getVarDomains(rule, constantChecker);
		List<TermModel> bodyModel = getBody(ruleHead.getName().getValue());
		addPossibilitiesToRuleHeadsBody(ruleHead.getBody(), bodyModel, possibleValues, rule, varDomains);
	}

	private void addPossibilitiesToRuleHeadsBody(List<GdlTerm> body,
			List<TermModel> bodyModel,
			Map<TermModel, Set<GdlConstant>> possibleValues, GdlRule rule,
			Map<GdlVariable, Set<GdlConstant>> varDomains) {
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlConstant) {
				possibleValues.get(termModel).add((GdlConstant) term);
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<TermModel> functionBodyModel = termModel.getFunction(function);
				addPossibilitiesToRuleHeadsBody(function.getBody(), functionBodyModel, possibleValues, rule, varDomains);
			} else if(term instanceof GdlVariable) {
				possibleValues.get(termModel).addAll(varDomains.get(term));
			} else {
				throw new RuntimeException("Unexpected GdlTerm type encountered");
			}
		}
	}

	private void addConstantsAsPossibleValues(List<GdlTerm> body,
			List<TermModel> bodyModel,
			Map<TermModel, Set<GdlConstant>> possibleValues) {
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlConstant) {
				possibleValues.get(termModel).add((GdlConstant) term);
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<TermModel> functionBodyModel = termModel.getFunction(function);
				addConstantsAsPossibleValues(function.getBody(), functionBodyModel, possibleValues);
			} else {
				throw new RuntimeException("Wasn't expecting a variable in a relation in the description");
			}
		}
	}

	private boolean applyNeedDomainRestriction(ConstantChecker constantChecker) throws InterruptedException {
		Map<TermModel, Set<GdlConstant>> neededValues = new HashMap<TermModel, Set<GdlConstant>>();
		//Grab the initially needed values
		addNeededValues(neededValues);
		//Propagate need until we agree on everything
		Map<TermModel, Set<GdlConstant>> oldValues = new HashMap<TermModel, Set<GdlConstant>>(neededValues);
		while(true) {
			if(Thread.currentThread().isInterrupted())
				throw new InterruptedException("Interrupted in SentenceModel:544");
			propagateNeededValues(neededValues, constantChecker);
			if(neededValues.equals(oldValues))
				break;
			//oldValues = new HashMap<TermModel, Set<GdlConstant>>(neededValues);
			oldValues = new HashMap<TermModel, Set<GdlConstant>>();
			for(TermModel termModel : neededValues.keySet()) {
				oldValues.put(termModel, new HashSet<GdlConstant>(neededValues.get(termModel)));
			}
		}

		boolean changedSomething = false;
		//Now we restrict the domains to the needed values
		for(TermModel termModel : getAllTermModels()) {
			if(Thread.currentThread().isInterrupted())
				throw new InterruptedException("Interrupted in SentenceModel:559");
			if(neededValues.containsKey(termModel)) {
				if(termModel.getConstants().retainAll(neededValues.get(termModel))) {
					changedSomething = true;
				}
			} else {
				//If there's no mapping, we saw no needed values
				//TODO: Find the form this term model is in, so this message can be a little bit useful
				//I think this message will be useful, but only with more info...
				//System.err.println("Possible error: No needed value found for term model "+termModel);
			}
		}

		/*System.out.println("New domains for sentence forms: ");
		for(String name : sentences.keySet()) {
			System.out.print(name + ": ");
			for(TermModel termModel : sentences.get(name))
				System.out.print(termModel + ", ");
			System.out.println();
		}*/

		return changedSomething;

		//So, we should have a new domain for each term model, I guess...
		/*Map<TermModel, Set<GdlConstant>> newDomains = new HashMap<TermModel, Set<GdlConstant>>();
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
		}*/
	}

	private List<TermModel> getAllTermModels() {
		List<TermModel> allModels = new ArrayList<TermModel>();
		for(List<TermModel> bodyModel : sentences.values()) {
			addTermModels(bodyModel, allModels);
		}
		return allModels;
	}

	private void addTermModels(List<TermModel> bodyModel,
			List<TermModel> allModels) {
		for(TermModel termModel : bodyModel) {
			allModels.add(termModel);
			for(List<TermModel> functionBodyModel : termModel.getFunctions().values())
				addTermModels(functionBodyModel, allModels);
		}
	}

	private void propagateNeededValues(
			Map<TermModel, Set<GdlConstant>> neededValues,
			ConstantChecker constantChecker) throws InterruptedException {
		//We propagate need as follows:
		//If a term that is a variable in the head of a rule needs a
		//particular value, AND that variable is possible (i.e. in the
		//current domain) in every appearance of the variable in
		//positive conjuncts in the rule's body, then the value is
		//needed in every appearance of the variable in the rule
		//(positive or negative).

		//There is a second form of propagation:
		//If a variable does not appear in the head of a variable,
		//then all the values that are in the intersections of all the
		//domains from the positive conjuncts containing the variable
		//become needed.

		//Can we combine these?
		//Essentially, we want to do something for each rule, for each variable:
		//1) Compute the shared domain of the variable
		//2) Turn that into the set of potential needed values
		//3) If it appears in the head, retain only those values needed somewhere in the head
		for(Gdl gdl : description) {
			if(Thread.currentThread().isInterrupted())
				throw new InterruptedException("Interrupted in SentenceModel:642");

			if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				Set<GdlVariable> vars = new HashSet<GdlVariable>(GdlUtils.getVariables(rule));
				for(GdlVariable var : vars) {
					propagateNeededValuesInRule(rule, var, neededValues, constantChecker);
				}
			}
		}
	}

	private void propagateNeededValuesInRule(GdlRule rule, GdlVariable var,
			Map<TermModel, Set<GdlConstant>> neededValues,
			ConstantChecker constantChecker) throws InterruptedException {
		//TODO: Implement
		//1) Compute the shared domain of the variable (the intersection of the
		//   domains of the term models where the variable appears)
		//This is collecting only those appearances that are in positive
		//conjuncts, which define the domain.
		Set<GdlConstant> domain = getVarDomains(rule, constantChecker).get(var);
		//2) Turn that into the set of potential needed values
		//3) If it appears in the head, retain only those values needed somewhere in the head
		//So, we need to collect the needed instances in the head.
		//And, uh, figure out if it appears at all in the head.
		if(GdlUtils.containsTerm(rule.getHead(), var)) {
			Set<GdlConstant> neededValuesInHead = findAllNeededValuesInHead((GdlRelation) rule.getHead(), var, neededValues);
			domain.retainAll(neededValuesInHead);
		}
		//4) Add this domain to the needed values for every term model
		//   representing a term that is this variable (in the rule body)
		for(GdlSentence sentence : GdlUtils.getSentencesInRuleBody(rule)) {
			if(sentence instanceof GdlRelation) {
				//List<TermModel> bodyModel = getBody(sentence.getName().getValue());
				//addToNeededValuesForVarTerms(sentence.getBody(), bodyModel, var, domain);
				for(TermModel termModel : getMatchingTermModels(var, (GdlRelation) sentence)) {
					if(!neededValues.containsKey(termModel))
						neededValues.put(termModel, new HashSet<GdlConstant>());
					neededValues.get(termModel).addAll(domain);
				}
			}
			ConcurrencyUtils.checkForInterruption();
		}
	}



	private Set<GdlConstant> findAllNeededValuesInHead(GdlRelation head,
			GdlVariable var, Map<TermModel, Set<GdlConstant>> neededValues) {
		//For each appearance in the head, get the needed values
		//then take the union, NOT the intersection
		Set<GdlConstant> result = new HashSet<GdlConstant>();
		for(TermModel termModel : getMatchingTermModels(var, head)) {
			if(neededValues.containsKey(termModel))
				result.addAll(neededValues.get(termModel));
		}
		return result;
	}

	private void addNeededValues(Map<TermModel, Set<GdlConstant>> neededValues) {
		//This is initially populated in two ways:
		//1) If the term model is part of a next/goal/legal sentence,
		//   then it is needed. (Advanced: We can use base/input to
		//   throw out some of these values.) Um, I suppose this goes
		//   for does/true/role, as well.
		for(Entry<String, List<TermModel>> entry : sentences.entrySet()) {
			String name = entry.getKey();
			if(!name.equals("next")
					&& !name.equals("goal")
					&& !name.equals("legal")
					&& !name.equals("does")
					&& !name.equals("true")
					&& !name.equals("role")
					&& !name.equals("init")
					&& !name.equals("base")
					&& !name.equals("input"))
				continue;
			//Crawl through the term model, add everything
			addAllValuesToNeededList(entry.getValue(), neededValues);
		}
		//2) If the term has a constant value in some sentence in the
		//   BODY of a rule, then it is needed.
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				addNeededConstantsFromRuleBody(rule, neededValues);
			}
		}
	}

	private void addNeededConstantsFromRuleBody(GdlRule rule,
			Map<TermModel, Set<GdlConstant>> neededValues) {
		//Find every sentence in the body of the rule
		for(GdlSentence sentence : GdlUtils.getSentencesInRuleBody(rule)) {
			if(sentence instanceof GdlRelation) {
				//Crawl through the rule; when we find a constant,
				//add it to the appropriate TermModel
				List<TermModel> modelBody = getBody(sentence.getName().getValue());
				if(modelBody == null) {
					System.out.println(sentence);
					System.out.println(sentences);
				}
				addNeededConstantsFromSentenceBody(sentence.getBody(), modelBody, neededValues);
			}
		}
	}


	//Also works with function bodies.
	private void addNeededConstantsFromSentenceBody(List<GdlTerm> body,
			List<TermModel> bodyModel,
			Map<TermModel, Set<GdlConstant>> neededValues) {
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlConstant) {
				if(!neededValues.containsKey(termModel))
					neededValues.put(termModel, new HashSet<GdlConstant>());
				neededValues.get(termModel).add((GdlConstant)term);
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<TermModel> functionBodyModel = termModel.getFunction(function);
				addNeededConstantsFromSentenceBody(function.getBody(), functionBodyModel, neededValues);
			}
		}
	}

	private void addAllValuesToNeededList(List<TermModel> bodyModel,
			Map<TermModel, Set<GdlConstant>> neededValues) {
		for(TermModel termModel : bodyModel) {
			if(!neededValues.containsKey(termModel))
				neededValues.put(termModel, new HashSet<GdlConstant>());
			neededValues.get(termModel).addAll(termModel.getConstants());
			for(List<TermModel> functionBody : termModel.getFunctions().values())
				addAllValuesToNeededList(functionBody, neededValues);
		}
	}

	//This could probably be factored out into some utility class.
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

	private Map<SentenceForm, Set<SentenceForm>> dependencyGraph = null;
	/**
	 * Each key in the graph depends on those sentence forms in the associated set.
	 */
	@Override
	public Map<SentenceForm, Set<SentenceForm>> getDependencyGraph() {
		if(dependencyGraph == null) {
			//Build graph from rules
			dependencyGraph = new HashMap<SentenceForm, Set<SentenceForm>>();
			for(Gdl gdl : description) {
				if(gdl instanceof GdlRule) {
					GdlRule rule = (GdlRule) gdl;
					SentenceForm head = new SentenceFormImpl(rule.getHead());
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
	@Override
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
	@Override
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
						SentenceForm trueForm = candidate.getCopyWithName(GdlPool.getConstant("true"));
						changing.add(trueForm);
						unfollowed.add(trueForm);
					}
				}
			}
		}
		return changing;
	}

	private Set<SentenceForm> extractSentenceForms(Gdl gdl) {
		final Set<SentenceForm> forms = new HashSet<SentenceForm>();
		GdlVisitors.visitAll(gdl, new GdlVisitor() {
			@Override
			public void visitSentence(GdlSentence sentence) {
				forms.add(new SentenceFormImpl(sentence));
			}
		});
		return forms;
	}

	public class SentenceFormImpl implements SentenceForm {
		public SentenceFormImpl(GdlSentence sentence) {
			sentenceName = sentence.getName();
			functionalElements = new ArrayList<GdlConstant>();
			arities = new ArrayList<Integer>();
			tupleSize = 0; //Gets filled in as we go
			if(sentence instanceof GdlProposition)
				return;
			GdlRelation relation = (GdlRelation) sentence;
			fillElementsWithBody(relation.getBody(), 0);
		}
		public SentenceFormImpl(SentenceFormImpl original, GdlConstant name) {
			//Construct a SentenceForm based on another, but with a different relation name
			sentenceName = name;
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
		private final GdlConstant sentenceName;
		private final List<GdlConstant> functionalElements;
		private final List<Integer> arities;

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
			SentenceFormImpl other = (SentenceFormImpl) obj;
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

		@Override
		public GdlConstant getName() {
			return sentenceName;
		}
		@Override
		public SentenceForm getCopyWithName(GdlConstant name) {
			return new SentenceFormImpl(this, name);
		}
		@Override
		public boolean matches(GdlSentence sentence) {
			//Does the sentence fit the form? Hmm...
			if(sentence == null)
				return false;
			if(sentence.getName() != sentenceName)
				return false;
			if(sentence instanceof GdlProposition)
				return functionalElements.isEmpty();
			GdlRelation relation = (GdlRelation) sentence;
			Iterator<GdlConstant> elementItr = functionalElements.iterator();
			Iterator<Integer> arityItr = arities.iterator();
			return elementsMatchBody(relation.getBody(), elementItr, arityItr);
		}
		private boolean elementsMatchBody(List<GdlTerm> body, Iterator<GdlConstant> elementItr, Iterator<Integer> arityItr) {
			//Go through the body, increase the index as we go
			for(GdlTerm term : body) {
				if(term instanceof GdlFunction) {
					GdlFunction function = (GdlFunction) term;
					if(elementItr.next() != function.getName()) {
						return false;
					}
					if(arityItr.next() != function.arity()) {
						return false;
					}
					//Check the function body
					if(!elementsMatchBody(function.getBody(), elementItr, arityItr)) {
						return false;
					}
				} else {
					if(elementItr.next() != null)
						return false;
					arityItr.next();
				}
			}
			return true;
		}

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
						if (functionBody == null) {
							throw new NullPointerException("Function body was null. Function name: " + functionName + "; sentence name: " + sentenceName);
						}
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
			Iterator<GdlConstant> elementItr = functionalElements.iterator();
			Iterator<Integer> arityItr = arities.iterator();
			Iterator<GdlConstant> tupleItr = tuple.iterator();
			List<GdlTerm> body = new ArrayList<GdlTerm>();
			//we could bootstrap the arity...
			int arity = arities.size();
			for(int a : arities)
				arity -= a;
			fillBody(body, elementItr, arityItr, tupleItr, arity);
			return GdlPool.getRelation(sentenceName, body);
		}
		private void fillBody(List<GdlTerm> body, Iterator<GdlConstant> elementItr,
				Iterator<Integer> arityItr, Iterator<GdlConstant> tupleItr, int arity) {
			for(int i = 0; i < arity; i++) {
				//fill in the ith element of the body
				//starting with the index1th element of functionalElements
				//constants filled by the index2th element of tuple
				GdlConstant functionName = elementItr.next();
				int a = arityItr.next();
				if(functionName == null) {
					body.add(tupleItr.next());
				} else {
					//add the function
					List<GdlTerm> functionBody = new ArrayList<GdlTerm>(a);
					fillBody(functionBody, elementItr, arityItr, tupleItr, a);
					body.add(GdlPool.getFunction(functionName, functionBody));
				}
			}
		}
		@Override
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

	@Override
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
		if(!sentences.containsKey(newName))
			sentences.put(newName, TermModel.copy(sentences.get(sentenceToCopy)));
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
	@Override
	public Set<SentenceForm> getSentenceForms() {
		if(sentenceForms == null) {
			sentenceForms = new HashSet<SentenceForm>();
			for(Gdl gdl : description) {
				sentenceForms.addAll(extractSentenceForms(gdl));
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
				formsToAdd.add(form.getCopyWithName(GdlPool.getConstant("true")));
			else if(form.getName().getValue().equals("init"))
				formsToAdd.add(form.getCopyWithName(GdlPool.getConstant("true")));
			else if(form.getName().getValue().equals("legal"))
				formsToAdd.add(form.getCopyWithName(GdlPool.getConstant("does")));
		}
		forms.addAll(formsToAdd);
	}

	private Map<SentenceForm, Set<GdlRelation>> relationsByForm = new HashMap<SentenceForm, Set<GdlRelation>>();
	@Override
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

	private Map<SentenceForm, Set<GdlRule>> rulesByForm = new HashMap<SentenceForm, Set<GdlRule>>();
	/**
	 * Returns the rules that GENERATE the sentence form, not necessarily
	 * all the rules that contain it.
	 *
	 * Note that if functions can be assigned to variables, this might not
	 * find all the rules capable of generating sentences of the given form.
	 */
	@Override
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

	@Override
	public SentenceForm getSentenceForm(GdlSentence sentence) {
		Set<SentenceForm> sentenceFormsWithName = getSentenceFormsWithName(sentence.getName());
		if (sentenceFormsWithName == null)
			return null;
		for(SentenceForm form : sentenceFormsWithName)
			if(form.matches(sentence))
				return form;
		return null;
	}

	private Map<GdlConstant, Set<SentenceForm>> sentenceFormsByName = null;
	public Set<SentenceForm> getSentenceFormsWithName(GdlConstant name) {
		if(sentenceFormsByName == null) {
			//Build it
			sentenceFormsByName = new HashMap<GdlConstant, Set<SentenceForm>>();
			for(SentenceForm form : getSentenceForms()) {
				GdlConstant formName = form.getName();
				if(!sentenceFormsByName.containsKey(formName))
					sentenceFormsByName.put(formName, new HashSet<SentenceForm>());
				sentenceFormsByName.get(formName).add(form);
			}
		}
		return sentenceFormsByName.get(name);
	}

	@Override
	public List<Gdl> getDescription() {
		return Collections.unmodifiableList(description);
	}

	/**
	 * Replaces the given rules with a set of new rules that must be in
	 * some sense equivalent to the old rules.
	 *
	 * Comparing the old and new descriptions, these properties must hold:
	 * - In every state, the sentences of those sentence forms defined in
	 *   the original description have the same truth value in each description.
	 * - There may be a new sentence form or multiple new sentence forms in
	 *   the new description; these must not be sentence forms with a name that
	 *   has special meaning in GDL (e.g. true, legal, goal, base, etc.).
	 *
	 * This allows us to efficiently update the model by simply adding the
	 * new sentence form and leaving the rest of the model intact. If more
	 * changes are needed, consider creating a new SentenceModel with the
	 * full description instead.
	 *
	 * TODO: Major perf issues here (memory, basically unbounded loops)
	 *
	 * @param oldRules Rules to be removed from the model.
	 * @param newRules Semantically equivalent rules to replace the old rules.
	 * @throws InterruptedException
	 */
	@Override
	public void replaceRules(List<GdlRule> oldRules, List<GdlRule> newRules) throws InterruptedException {
		//Step 1: Identify the new sentence forms
		Set<SentenceForm> newForms = new HashSet<SentenceForm>();
		for(GdlRule newRule : newRules)
			newForms.addAll(extractSentenceForms(newRule));
		newForms.removeAll(getSentenceForms());

		//Step 2: Replace the rules
		description.removeAll(oldRules);
		description.addAll(newRules);

		//Step 3: Add the new sentence forms (and rules) everywhere needed
		//Need to update:
		//sentences
		//We use the same injection method (and infrastructure) we used earlier.
		boolean somethingChanged = true;
		while(somethingChanged) {
			somethingChanged = false;
			for(GdlRule newRule : newRules) {
				somethingChanged |= applyInjection(newRule);
			}
			//Continue until nothing changes in any rule
		}
		//maxArity: unneeded?
		//TODO: Better method for dependencyGraph
		dependencyGraph = null;
		//TODO: Better methods for these
		//independentSentenceForms
		independentSentenceForms = null;
		//constantSentenceForms
		constantSentenceForms = null;
		//dependentSentenceForms
		dependentSentenceForms = null;
		//sentenceForms
		//if(sentenceForms != null)
		//	sentenceForms.addAll(newForms);
		//We're storing this as an unmodifiable set, so replace instead
		sentenceForms = null;
		//relationsByForm: unchanged
		//rulesByForm
		for(GdlRule oldRule : oldRules) {
			SentenceForm headForm = getSentenceForm(oldRule.getHead());
			if(rulesByForm.get(headForm) != null) {
				rulesByForm.get(headForm).remove(oldRule);
			}
		}
		for(GdlRule newRule : newRules) {
			SentenceForm headForm = getSentenceForm(newRule.getHead());
			//This should be an uncommon occurrence...
			if(rulesByForm.get(headForm) != null)
				rulesByForm.get(headForm).add(newRule);
		}
		//sentenceFormsByName
		if(sentenceFormsByName != null) {
			for(SentenceForm form : newForms) {
				GdlConstant name = form.getName();
				if(!sentenceFormsByName.containsKey(name))
					sentenceFormsByName.put(name, new HashSet<SentenceForm>());
				sentenceFormsByName.get(name).add(form);
			}
		}
	}

	public Set<GdlConstant> getDomainOfVarInRelation(GdlVariable var,
			GdlRelation relation) {
		//Find the intersection of all the places it appears
		List<TermModel> termModels = getMatchingTermModels(var, relation);
		Set<GdlConstant> domain = new HashSet<GdlConstant>();
		if(termModels.isEmpty()) {
			//Not in the relation
			System.err.println("Error: Tried to find the domain of " + var + " in relation " + relation);
			return Collections.emptySet();
		}
		for(TermModel termModel : termModels) {
			if(domain.isEmpty()) {
				domain.addAll(termModel.getConstants());
			} else {
				domain.retainAll(termModel.getConstants());
				if(domain.isEmpty())
					return domain;
			}
		}
		return domain;
	}

	/**
	 * Finds all the instances of a particular term (variable or
	 * constant) in a given sentence and returns the term models
	 * associated with them. Used to find the domain of a variable
	 * in a sentence.
	 *
	 * @param term
	 * @param relation
	 * @return
	 */
	private List<TermModel> getMatchingTermModels(GdlTerm term,
			GdlRelation relation) {
		List<TermModel> matches = new ArrayList<TermModel>();
		//Walk through the form and relation together
		String sentenceName = relation.getName().getValue();
		List<TermModel> bodyModel = sentences.get(sentenceName);
		List<GdlTerm> body = relation.getBody();
		getMatchingTermModels(body, bodyModel, term, matches);
		return matches;
	}

	private void getMatchingTermModels(List<GdlTerm> body,
			List<TermModel> bodyModel, GdlTerm toMatch, List<TermModel> matches) {
		for(int i = 0; i < body.size(); i++) {
			GdlTerm term = body.get(i);
			TermModel termModel = bodyModel.get(i);
			if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<GdlTerm> functionBody = function.getBody();
				List<TermModel> functionBodyModel = termModel.getFunction(function);
				getMatchingTermModels(functionBody, functionBodyModel, toMatch, matches);
			} else {
				if(term.equals(toMatch)) {
					matches.add(termModel);
				}
			}
		}
	}

	@Override
	public Map<GdlVariable, Set<GdlConstant>> getVarDomains(GdlRule rule) {
		return getVarDomains(rule, null);
	}
	public Map<GdlVariable, Set<GdlConstant>> getVarDomains(GdlRule rule, ConstantChecker checker) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>(GdlUtils.getVariables(rule));
		Map<GdlVariable, Set<GdlConstant>> varDomains = new HashMap<GdlVariable, Set<GdlConstant>>();
		for(GdlLiteral conjunct : rule.getBody()) {
			if(conjunct instanceof GdlRelation) {
				//This is where variables must be assigned
				for(GdlVariable var : vars) {
					Set<GdlConstant> domain = getDomainInRelation((GdlRelation) conjunct, var);
					//If this is a known partially-filled constant,
					//treat it differently!
					if(domain != null && checker != null
							&& checker.hasConstantForm((GdlRelation)conjunct)) {
						domain = checker.getDomainInPossibleCompletions((GdlRelation)conjunct, var);
					}
					if(domain != null) {
						//Add to the domain
						if(varDomains.get(var) == null) {
							varDomains.put(var, new HashSet<GdlConstant>());
							varDomains.get(var).addAll(domain);
						} else {
							varDomains.get(var).retainAll(domain);
							if(varDomains.get(var).isEmpty()) {
								//The game probably has an error in this rule
								//Why? Because with an empty domain, the rule does nothing
								System.out.println("Warning: Probable error in rule " + rule + ": check domains for variable " + var);
							}
						}
					}
				}
			}
		}
		//Do we want to take the intersection with the head's domain?
		//I guess so
		GdlSentence head = rule.getHead();
		if(head instanceof GdlRelation) {
			Map<GdlVariable, Set<GdlConstant>> headDomains = getDomainsInRelation((GdlRelation)head);
			for(GdlVariable var : headDomains.keySet()) {
				if(varDomains.get(var) == null) {
					System.out.println("Check game for unsafe rules...");
				}
				varDomains.get(var).retainAll(headDomains.get(var));
			}
		}
		return varDomains;
	}
	private Map<GdlVariable, Set<GdlConstant>> getDomainsInRelation(
			GdlRelation relation) {
		Map<GdlVariable, Set<GdlConstant>> domains = new HashMap<GdlVariable, Set<GdlConstant>>();
		Set<GdlVariable> vars = new HashSet<GdlVariable>(GdlUtils.getVariables(relation));
		for(GdlVariable var : vars)
			domains.put(var, getDomainInRelation(relation, var));
		return domains;
	}

	private Set<GdlConstant> getDomainInRelation(GdlRelation relation,
			GdlVariable var) {
		//Traverse the model and relation together
		Set<GdlConstant> domain = new HashSet<GdlConstant>();
		boolean found = setDomainInRelation(domain, relation.getBody(),
				this.getBody(relation.getName().getValue()), var);
		if(!found)
			return null;
		return domain;
	}
	private boolean setDomainInRelation(Set<GdlConstant> domain,
			List<GdlTerm> gdlBody, List<TermModel> modelBody, GdlVariable var) {
		boolean found = false;
		for(int i = 0; i < gdlBody.size(); i++) {
			//If the domain is actually empty, don't add anything else to it
			if(found && domain.isEmpty())
				return true;
			GdlTerm term = gdlBody.get(i);
			TermModel termModel = modelBody.get(i);
			if(term.equals(var)) {
				found = true;
				if(domain.isEmpty()) {
					domain.addAll(termModel.getConstants());
				} else {
					domain.retainAll(termModel.getConstants());
					if(domain.isEmpty())
						return true;
				}
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<TermModel> functionModelBody = termModel.getFunction(function);
				if(setDomainInRelation(domain, function.getBody(), functionModelBody, var))
					found = true;
			}
		}
		return found;
	}

	@Override
	public Iterator<GdlSentence> getSentenceIterator(SentenceForm form) {
		if (!(form instanceof SentenceFormImpl))
			throw new IllegalArgumentException("Must use a native SentenceForm");
		return ((SentenceFormImpl) form).iterator();
	}

	@Override
	public Iterable<GdlSentence> getSentenceIterable(final SentenceForm form) {
		return new Iterable<GdlSentence>() {
			@Override
			public Iterator<GdlSentence> iterator() {
				return getSentenceIterator(form);
			}
		};
	}

}
