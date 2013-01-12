package org.ggp.base.util.gdl.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceModelImpl;
import org.ggp.base.util.gdl.model.TermModel;


/**
 * 
 * @author Alex Landau
 *
 */
public class VariableConstrainer {
	/**
	 * Modifies a GDL description by replacing all rules in which variables could be bound to
	 * functions, so that the new rules will only bind constants to variables. Also automatically
	 * removes GdlOrs from the rules using the DeORer.
	 * 
	 * Not guaranteed to work if the GDL is written strangely, such as when they include rules
	 * in which certain conjuncts are never or always true. Not guaranteed to work when rules
	 * are unsafe, i.e., they contain variables only appearing in the head, a negated literal,
	 * and/or a distinct literal. (In fact, this can be a good way to test for GDL errors, which
	 * often result in exceptions.)
	 * 
	 * Not guaranteed to finish in a reasonable amount of time in pathological cases, where the
	 * number of possible functional structures is prohibitively large.
	 * 
	 * @param description A GDL game description.
	 * @return A modified version of the same game.
	 * @throws InterruptedException 
	 * @throws SimplifierException
	 */
	public static List<Gdl> replaceFunctionValuedVariables(List<Gdl> description) throws InterruptedException {
		//until we have "or mode" working, do this first
		List<Gdl> deoredDescription = DeORer.run(description);

		SentenceModelImpl model = new SentenceModelImpl(deoredDescription);

		List<Gdl> result = getVarLiteralSimplification(deoredDescription, model);

		return result;
	}

	private static List<Gdl> getVarLiteralSimplification(List<Gdl> description,
			SentenceModelImpl model) {
		//General strategy:
		//Look for rules with conjuncts on the RHS where variables
		//are in positions corresponding to TermModels with
		//functional terms available
		//These should be replaced with specific rules for the
		//different functions
		List<Gdl> newDescription = new ArrayList<Gdl>();
		
		for(Gdl gdl : description) {
			if(!(gdl instanceof GdlRule)) {
				newDescription.add(gdl);
			} else {
				GdlRule rule = (GdlRule) gdl;
				//Look for variables in the conjuncts
				//Crawl though the model and the rule together
				List<GdlVariable> variablesInRule = GdlUtils.getVariables(rule);
				Set<GdlVariable> varsToReplace = new HashSet<GdlVariable>();
				Map<GdlVariable, List<TermModel>> varModels = new HashMap<GdlVariable, List<TermModel>>();
				for(GdlVariable var : variablesInRule)
					varModels.put(var, new ArrayList<TermModel>());
				for(int i = 0; i < rule.arity(); i++) {
					GdlLiteral conjunct = rule.get(i);
					//Get the appropriate body model
					processConjunct(conjunct, model, varModels, varsToReplace);
				}
				//Now we have all the variables to replace and the term models
				Map<GdlVariable, TermModel> replacements = new HashMap<GdlVariable, TermModel>();
				for(GdlVariable var : varsToReplace) {
					//In "or" mode, replace this with union... is that all we need to do?
					TermModel replacementModel = TermModel.getIntersection(varModels.get(var));
					replacements.put(var, replacementModel);
				}
				//Now we figure out how to rewrite the rule.
				//In "compatibility mode", we want, for each variable, every possible
				//way of splitting it into components.
				rewriteAndRecordRule(rule, replacements, newDescription);
			}
		}
		return newDescription;
	}

	private static void rewriteAndRecordRule(GdlRule rule, 
			Map<GdlVariable, TermModel> replacements, List<Gdl> newDescription) {
		Set<String> usedVarNames = new HashSet<String>(GdlUtils.getVariableNames(rule));
		rewriteAndRecordRule(rule, replacements, newDescription, usedVarNames);
	}

	private static void rewriteAndRecordRule(GdlRule craftedRule,
			Map<GdlVariable, TermModel> replacements, List<Gdl> newDescription,
			Set<String> usedVarNames) {
		//If no replacements left to make, record the rule
		if(replacements.isEmpty()) {
			newDescription.add(craftedRule);
			return;
		}
		
		GdlVariable var = replacements.keySet().iterator().next();
		TermModel replacement = replacements.get(var);
		replacements.remove(var);
		for(GdlConstant c : replacement.getConstants()) {
			GdlRule tempRule = CommonTransforms.replaceVariable(craftedRule, var, c);
			rewriteAndRecordRule(tempRule, replacements, newDescription, usedVarNames);
		}
		for(Entry<String, List<TermModel>> f : replacement.getFunctions().entrySet()) {
			//crafted functions
			List<GdlFunction> cfs = getCraftableFunctions(f, usedVarNames);
			for(GdlFunction cf : cfs) {
				GdlRule tempRule = CommonTransforms.replaceVariable(craftedRule, var, cf);
				rewriteAndRecordRule(tempRule, replacements, newDescription, usedVarNames);
			}
		}
		
		replacements.put(var, replacement);
	}

	private static List<GdlFunction> getCraftableFunctions(
			Entry<String, List<TermModel>> f, Set<String> usedVarNames) {
		//Here we get the GdlFunctions that could match the function model
		//(without including any function-valued variables)
		//String functionKey = f.getKey();
		//String functionName = functionKey.substring(0, functionKey.lastIndexOf("_"));
		String functionName = f.getKey();
		List<TermModel> bodyModel = f.getValue();
		List<GdlTerm> functionBodySoFar = new ArrayList<GdlTerm>();
		//We recursively fill in the terms of the function according to the model
		List<GdlFunction> craftableFunctions = new ArrayList<GdlFunction>();
		craftFunctionTerm(functionBodySoFar, bodyModel, 0, craftableFunctions, usedVarNames, GdlPool.getConstant(functionName));
		return craftableFunctions;
	}

	private static void craftFunctionTerm(List<GdlTerm> functionBodySoFar,
			List<TermModel> bodyModel, int i, List<GdlFunction> craftableFunctions,
			Set<String> usedVarNames, GdlConstant functionName) {
		if(i == bodyModel.size()) {
			craftableFunctions.add(GdlPool.getFunction(functionName, functionBodySoFar));
			return;
		}
		//We need to go through all the possibilities for term number i
		TermModel term = bodyModel.get(i);
		//Easy case: No functions; just replace with a variable
		if(!term.hasFunctions()) {
			GdlVariable newVar = getUnusedVariable(usedVarNames);
			functionBodySoFar.add(newVar);
			craftFunctionTerm(functionBodySoFar, bodyModel, i+1, craftableFunctions, usedVarNames, functionName);
		} else {
			//Hard case: One for each constant, recurse for each function (in compatibility mode)
			functionBodySoFar.add(null);
			//Add the cases for individual constants instead of variables
			//Otherwise, state machines that do assign functions to variables
			//could conceivably get confused
			for(GdlConstant c : term.getConstants()) {
				functionBodySoFar.set(i, c);
				craftFunctionTerm(functionBodySoFar, bodyModel, i+1, craftableFunctions, usedVarNames, functionName);
			}
			for(Entry<String, List<TermModel>> f : term.getFunctions().entrySet()) {
				List<GdlFunction> cfs = getCraftableFunctions(f, usedVarNames);
				for(GdlFunction cfTerm : cfs) {
					functionBodySoFar.set(i, cfTerm);
					craftFunctionTerm(functionBodySoFar, bodyModel, i+1, craftableFunctions, usedVarNames, functionName);
				}
			}
		}
	}

	static int nextVarNum = 1;
	private static GdlVariable getUnusedVariable(Set<String> usedVarNames) {
		String candidateName = "?a" + nextVarNum;
		nextVarNum++;
		while(usedVarNames.contains(candidateName)) {
			candidateName = "?a" + nextVarNum;
			nextVarNum++;
		}
		usedVarNames.add(candidateName);
		return GdlPool.getVariable(candidateName);
	}

	private static void processConjunct(GdlLiteral conjunct, SentenceModelImpl model, Map<GdlVariable, List<TermModel>> varModels, Set<GdlVariable> varsToReplace) {
		if(conjunct instanceof GdlSentence) {
			GdlSentence sentence = (GdlSentence) conjunct;
			List<GdlTerm> conjunctBody;
			try {conjunctBody = sentence.getBody();} catch(RuntimeException e) {conjunctBody = Collections.emptyList();}
			List<TermModel> modelBody = model.getBodyForSentence(sentence);
			//This is useful information for debugging a faulty GDL file.
			if(modelBody == null)
				System.out.println("model body null: " + sentence + "; " + model);
			
			searchForBoundFunctions(conjunctBody, modelBody, varModels, varsToReplace);
		} else if(conjunct instanceof GdlNot) {
			//Nothing is needed here, as we will find the variable's functional value
			//in a positive literal (if the game has no unsafe rules).
		} else if(conjunct instanceof GdlOr) {
			GdlOr or = (GdlOr) conjunct;
			for(int i = 0; i < or.arity(); i++)
				processConjunct(or.get(i), model, varModels, varsToReplace);
		} else if(conjunct instanceof GdlDistinct) {
			//Do I do anything here?
			//There may be cases where a variable is being compared to a function
			//(or two variables are checked to see if they are the same function).
			//If the GDL is well-formed, those variables will appear in other
			//conjuncts in the rule; if those conjuncts are at all useful, the
			//variables models will indicate the functional value in those other
			//conjuncts, so we will catch and expand the variable.
			//So, we don't need to do anything here.
		} else {
			throw new RuntimeException("The GDL specification has changed since this was built");
		}
	}

	private static void searchForBoundFunctions(List<GdlTerm> body,
			List<TermModel> bodyModel, Map<GdlVariable, List<TermModel>> varModels, Set<GdlVariable> varsToReplace) {
		//Look at each term for our goal
		for(int t = 0; t < body.size(); t++) {
			GdlTerm term = body.get(t);
			TermModel termModel = bodyModel.get(t);
			if(term instanceof GdlVariable) {
				varModels.get(term).add(termModel);
				//See if there are functional candidates
				if(termModel.hasFunctions()) {
					varsToReplace.add((GdlVariable)term);
				}
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<GdlTerm> functionBody = function.getBody();
				List<TermModel> functionBodyModel = termModel.getFunction(function);
				
				if(functionBodyModel == null)
					System.out.println("function model body null: " + body + "; " + functionBodyModel);
				
				searchForBoundFunctions(functionBody, functionBodyModel, varModels, varsToReplace);
			}
		}
		
	}
}
