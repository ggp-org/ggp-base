package org.ggp.base.util.gdl.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceModelImpl;


public class SimpleCondensationIsolator {
	/*
	 * The purpose of CondensationIsolater is to rewrite rules to make
	 * them easier for propnets to handle. Other forward-chaining
	 * approaches may also find this useful.
	 * 
	 * "Condensation" refers to rules that contain variables not in the
	 * head. For example, we might have a rule
	 * (<= (threatened ?x2 ?y2)
	 *     (threatens ?x1 ?y1 ?x2 ?y2))
	 * which "condenses" a potentially large number of pairs of ?x1 and ?y1
	 * into a single statement that may be more useful elsewhere on the
	 * board.
	 * 
	 * - Alex Landau
	 */
	
	public static List<Gdl> run(List<Gdl> description, boolean experimental) throws InterruptedException {
		//This is much easier on a deORed description
		description = DeORer.run(description);
		
		List<Gdl> newDescription = new ArrayList<Gdl>();
		List<GdlRule> rules = new ArrayList<GdlRule>();
		
		//First, separate into relations (which go unchanged) and rules
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule) {
				rules.add((GdlRule) gdl);
			} else {
				newDescription.add(gdl);
			}
		}
		Set<String> sentenceNames = new SentenceModelImpl(description).getSentenceNames();
		
		//the list of rules might grow as we go
		for(int i = 0; i < rules.size(); i++) {
			GdlRule rule = rules.get(i);
			//See if this rule contains a condensation that can be factored out
			Condensation condensation;
			if(experimental) {
				condensation = Condensation.getCondensation2(rule, rules, sentenceNames);
				if(condensation == null)
					condensation = Condensation.getCondensation(rule, rules, sentenceNames);
			} else {
				condensation = Condensation.getCondensation(rule, rules, sentenceNames);
			}
			if(condensation != null) {
				//Modify the rule
				rules.set(i, condensation.getModifiedRule());
				i--; //Go over the rule again
				if(condensation.hasNewRule()) {
					rules.add(condensation.getNewRule());
				}
			}
		}
		newDescription.addAll(rules);
		return newDescription;
	}
	
	private static class Condensation {
		GdlRule modifiedRule;
		//We generate the new rule, but then we check to see if there's already
		//some other rule that matches it. If not, this stays null.
		GdlRule newRule = null;
		
		public Condensation(Set<GdlVariable> condensationVars, GdlRule rule,
				List<GdlRule> rules, Set<String> sentenceNames) {
			//This condensation has to:
			//- Modify the rule to remove all components with the given variables 
			//- Do one of the following:
			//  - Create a new rule combining those variables
			//  - Find an existing rule 
			//It seems reasonable to do the second step first.
			
			//Even if we go looking for an existing rule, we need something to compare it to
			//The head should contain all the vars in the relevant literals
			List<GdlLiteral> modifiedBody = new ArrayList<GdlLiteral>();
			List<GdlLiteral> condenserBody = new ArrayList<GdlLiteral>();
			for(GdlLiteral literal : rule.getBody()) {
				if(Collections.disjoint(GdlUtils.getVariables(literal), condensationVars))
					modifiedBody.add(literal);
				else
					condenserBody.add(literal);
			}
			
			//Now we want to make the condenser rule
			//First we have to get all the right variables in the head
			Set<GdlVariable> condenserHeadVars = new HashSet<GdlVariable>();
			for(GdlLiteral literal : condenserBody)
				condenserHeadVars.addAll(GdlUtils.getVariables(literal));
			condenserHeadVars.removeAll(condensationVars);
			//TODO: Do comparisons
			
			//If nothing found, name the rule
			GdlConstant condenserName;
			for(int i = 0; ; i++) {
				String candidateName = rule.getHead().getName().getValue() + "_tmp" + i;
				if(!sentenceNames.contains(candidateName)) {
					condenserName = GdlPool.getConstant(candidateName);
					sentenceNames.add(candidateName);
					//Success!
					break;
				}
			}
			//Make the rule head
			List<GdlTerm> orderedVars = new ArrayList<GdlTerm>(condenserHeadVars);
			GdlSentence condenserHead;
			if(orderedVars.isEmpty()) {
				condenserHead = GdlPool.getProposition(condenserName);
			} else {
				condenserHead = GdlPool.getRelation(condenserName, orderedVars);
			}
			//Make the condenser rule and add it
			//GdlRule condenserRule = GdlPool.getRule(condenserHead, condenserBody);
			newRule = GdlPool.getRule(condenserHead, condenserBody);
			
			//Add the condenser relation to the modified rule
			modifiedBody.add(condenserHead);
			modifiedRule = GdlPool.getRule(rule.getHead(), modifiedBody);
		}

		public GdlRule getNewRule() {
			return newRule;
		}

		public boolean hasNewRule() {
			return (newRule != null);
		}

		public GdlRule getModifiedRule() {
			return modifiedRule;
		}
		
		public static Condensation getCondensation(GdlRule rule, List<GdlRule> rules, Set<String> sentenceNames) {
			//First: Check that it has at least one
			int relevantCount = 0;
			for(GdlLiteral literal : rule.getBody()) {
				if(literal instanceof GdlSentence || literal instanceof GdlNot)
					relevantCount++;
			}
			if(relevantCount < 2)
				return null;
			
			//We're now looking for variables that appear in only one literal
			//Recent slight change: if there's a distinct clause with
			//only one variable in it, ignore that variable for these purposes.
			Set<GdlVariable> singleUseVars = new HashSet<GdlVariable>();
			Set<GdlVariable> multiUseVars = new HashSet<GdlVariable>();
			
			GdlSentence head = rule.getHead();
			multiUseVars.addAll(GdlUtils.getVariables(head));
			//Go through the body
			for(GdlLiteral literal : rule.getBody()) {
				Set<GdlVariable> usedVars = new HashSet<GdlVariable>(GdlUtils.getVariables(literal));
				//See chinesecheckers4.kif for example of this being helpful
				if(literal instanceof GdlDistinct && usedVars.size() == 1)
					continue;
				for(GdlVariable var : usedVars) {
					if(multiUseVars.contains(var)) {
						//do nothing
					} else if(singleUseVars.contains(var)) {
						singleUseVars.remove(var);
						multiUseVars.add(var);
					} else {
						singleUseVars.add(var);
					}
				}
			}
			
			//Do we have any single use vars?
			if(singleUseVars.isEmpty())
				return null;
			
			GdlVariable varChosen = singleUseVars.iterator().next();
			
			//Find the body literal it's in
			for(GdlLiteral literal : rule.getBody()) {
				List<GdlVariable> varsInLiteral = GdlUtils.getVariables(literal); 
				if(varsInLiteral.contains(varChosen)) {
					//Get all the single-use variables
					singleUseVars.retainAll(varsInLiteral);
					return new Condensation(singleUseVars, rule, rules, sentenceNames);
				}
			}
			
			//Shouldn't happen?
			return null;
		}

		public static Condensation getCondensation2(GdlRule rule, List<GdlRule> rules, Set<String> sentenceNames) {
			/* We now take the following approach to finding a useful condensation: 
			 * We start with a single candidate variable not found in the head.
			 * We add all the literals with that variable.
			 * Whenever we add a literal, we add all the non-head vars in that literal;
			 * whenever we add a variable, we add all the literals containing it.
			 * We stop when there's nothing left to add.
			 * If we end up with the entire set of literals, there's no condensation;
			 * otherwise, we generate a condensation of those vars and literals.
			 * Am I missing any cases?
			 * Yes, plenty
			 * What if we just gathered all the literals associated with the
			 * first variable, then used all the variables that only appear
			 * in those 
			 * That would have problems with examples like
			 * (<= (r1 x)
			 *     (r2 x a b)
			 *     (r3 x a c)
			 *     (not (r4 x b c)))
			 * In this case, we need both r2 and r3 if we're to factor out r4
			 * We could, however, factor out r2 and r3 to get:
			 * (<= (r5 x b c)
			 *     (r2 x a b)
			 *     (r3 x a c))
			 * (<= (r1 x)
			 *     (r5 x b c)
			 *     (not (r4 x b c)))
			 * How could we find this case?
			 * Only by explicitly focusing on factoring out a
			 * It's worth factoring out because it doesn't appear across all literals
			 * It's feasible to factor out because it doesn't include a not
			 * or distinct with other variables in it
			 * 
			 * But sometimes we want to look at pairs of variables, don't we?
			 * 
			 * (<= (r1 x)
			 *     (r2 x a)
			 *     (r3 x a b)
			 *     (r4 x b)
			 *     (r5 x c d))
			 * We might like to factor this into:
			 * (<= (r6 x)
			 *     (r2 x a)
			 *     (r3 x a b)
			 *     (r4 x b))
			 * (<= (r7 x)
			 *     (r5 x c d))
			 * (<= (r1 x)
			 *     (r6 x)
			 *     (r7 x))
			 * On the other hand, this might be better:
			 * (<= (r8 x b)
			 *     (r2 x a)
			 *     (r3 x a b))
			 * (<= (r6 x)
			 *     (r8 x b)
			 *     (r4 x b))
			 * Which is better? Let's assume here that r8 wouldn't be referenced
			 * by other relations for other reasons.
			 * Assume a and b each have domain size n.
			 * Then in the first case, for each (r6 const), we get:
			 * - n^2 AND gates for the n^2 possible combinations
			 * - 3n^2 inputs to the AND gates
			 * - 1 OR gate leading to (r6 const)
			 * - n^2 outputs from the AND gates to the OR gate
			 * Total: 4n^2 links
			 * In the second case, again for a given constant:
			 * - For each value of b, leading to r8:
			 *   - n AND gates, each with 2 inputs
			 *   - Outputs to an OR gate for each one
			 *   Total: 3n
			 * - Total for r8: 3n^2 links
			 * - For r6:
			 * - n AND gates, two inputs each
			 * - one OR gate
			 * - 3n links
			 * - So a total of 3n^2 + 3n; if 3 < n, this is smaller
			 * So the more "thorough" method actually appears to be better
			 * for large numbers of constants (the more typical case).
			 * 
			 * This means we really do want to factor out just one variable at a time.
			 * It just has to be useful and feasible.
			 * It is useful if there is some literal in the rule body that it
			 * doesn't appear in.
			 * It is feasible if none of the distinct or not literals that
			 * contain the variable contain other variables not fully contained
			 * in the set to be factored out.
			 * But the set to be factored out is somewhat flexible...
			 * Let's look at another example
			 * (<= (r1 x)
			 *     (r2 x a)
			 *     (r3 x b)
			 *     (not (r4 x a b))
			 *     (r5 x c)
			 *     (r6 x d)
			 *     (not (r7 x c d)))
			 * We could even entwine the two halves with something like (r5 x a c),
			 * and it would still be worth factoring.
			 * So we do want to expand to other variables in the case of not/distinct,
			 * just not in the case of positive literals.
			 * And then, of course, if it becomes useless, we ___.
			 * Maybe we can start with the nots/distincts?
			 * Though just one positive literal/one distinct doesn't seem worth factoring out.
			 * 
			 * Okay, look at it this way:
			 * We have a graph with a node for each variable not in the head
			 * Each "not" or "distinct" creates links between all variables in
			 * that literal
			 * This generates connected components that are minimal condensations
			 * as long as they are useful, which requires having at least one
			 * non-"distinct" literal including at least one of the variables
			 */
			//First, we identify the variables in the head of the rule,
			//as well as the variables not in the head
			List<GdlVariable> headVars = GdlUtils.getVariables(rule.getHead());
			List<GdlVariable> allRules = GdlUtils.getVariables(rule);
			List<GdlVariable> nonHeadVars = new ArrayList<GdlVariable>(allRules);
			nonHeadVars.removeAll(headVars);
			
			
			//Let's try the graph approach
			Map<GdlVariable, Set<GdlVariable>> varGraph = new HashMap<GdlVariable, Set<GdlVariable>>();
			for(GdlVariable nonHeadVar : nonHeadVars) {
				varGraph.put(nonHeadVar, new HashSet<GdlVariable>());
			}
			//For each variable in the rule, add constraints
			for(GdlLiteral literal : rule.getBody()) {
				List<GdlVariable> vars = GdlUtils.getVariables(literal);
				vars.removeAll(headVars);
				//Add links between these vars
				for(GdlVariable var : vars) {
					varGraph.get(var).addAll(vars);
				}
			}
			//Now we separate the graph into connected components
			//Remember, nodes may be connected indirectly, so we need BFS to get the
			//whole component
			List<Set<GdlVariable>> connectedComponents = getConnectedComponents(varGraph);
			
			//Now we'll test each component to see if it's useful
			for(Set<GdlVariable> condensationVars : connectedComponents) {
				if(isUsefulCondensation(condensationVars, rule)) {
					return new Condensation(condensationVars, rule, rules, sentenceNames);
				}
			}
			return null;
		}

		private static boolean isUsefulCondensation(
				Set<GdlVariable> condensationVars, GdlRule rule) {
			//Look for a substantial (i.e. non-"distinct") literal in the rule body
			//that does not contain any of these variables
			for(GdlLiteral literal : rule.getBody()) {
				if(literal instanceof GdlSentence || literal instanceof GdlNot) {
					List<GdlVariable> varsInLiteral = GdlUtils.getVariables(literal);
					if(Collections.disjoint(condensationVars, varsInLiteral))
						return true;
				}
			}
			return false;
		}

		private static List<Set<GdlVariable>> getConnectedComponents(
				Map<GdlVariable, Set<GdlVariable>> graph) {
			List<Set<GdlVariable>> components = new ArrayList<Set<GdlVariable>>();
			Set<GdlVariable> varsAdded = new HashSet<GdlVariable>();
			
			for(GdlVariable key : graph.keySet()) {
				Set<GdlVariable> component = new HashSet<GdlVariable>();
				Queue<GdlVariable> varsToAdd = new LinkedList<GdlVariable>();
				if(!varsAdded.contains(key))
					varsToAdd.add(key);
				
				while(!varsToAdd.isEmpty()) {
					GdlVariable curVar = varsToAdd.remove();
					if(varsAdded.contains(curVar))
						continue;
					//Find the children
					Set<GdlVariable> children = graph.get(curVar);
					//Add those children that have not been handled
					for(GdlVariable child : children) {
						if(!varsAdded.contains(child))
							varsToAdd.add(child);
					}

					component.add(curVar);
					varsAdded.add(curVar);
				}
				if(!component.isEmpty())
					components.add(component);
			}
			
			return components;
		}
		
	}
}
