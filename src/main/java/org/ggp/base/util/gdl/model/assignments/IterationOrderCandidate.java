package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

//This class has a natural ordering that is inconsistent with equals.
public class IterationOrderCandidate implements Comparable<IterationOrderCandidate> {
		//Information specific to this ordering
		private List<Integer> sourceConjunctIndices; //Which conjuncts are we using as sources, and in what order?
		private List<GdlVariable> varOrdering; //In what order do we assign variables?
		private List<Integer> functionalConjunctIndices; //Same size as varOrdering
												//Index of conjunct if functional, -1 otherwise
		private List<Integer> varSources; //Same size as varOrdering
									//For each variable: Which source conjunct
									//originally contributes it? -1 if none
									//Becomes sourceResponsibleForVar

		//Information shared by the orderings
		//Presumably, this will also be used to construct the iterator to be used...
		private List<GdlVariable> varsToAssign;
		private List<GdlSentence> sourceConjunctCandidates;
		private List<Integer> sourceConjunctSizes; //same indexing as candidates
		private List<GdlSentence> functionalSentences;
		private List<FunctionInfo> functionalSentencesInfo; //Indexing same as functionalSentences
		private Map<GdlVariable, Integer> varDomainSizes;

		/**
		 * This constructor is for creating the start node of the
		 * search. No part of the ordering is specified.
		 *
		 * @param sourceConjunctCandidates
		 * @param sourceConjunctSizes
		 * @param functionalSentences
		 * @param functionalSentencesInfo
		 * @param allVars
		 * @param varDomainSizes
		 */
		public IterationOrderCandidate(
				List<GdlVariable> varsToAssign,
				List<GdlSentence> sourceConjunctCandidates,
				List<Integer> sourceConjunctSizes,
				List<GdlSentence> functionalSentences,
				List<FunctionInfo> functionalSentencesInfo,
				Map<GdlVariable, Integer> varDomainSizes) {
			sourceConjunctIndices = new ArrayList<Integer>();
			varOrdering = new ArrayList<GdlVariable>();
			functionalConjunctIndices = new ArrayList<Integer>();
			varSources = new ArrayList<Integer>();

			this.varsToAssign = varsToAssign;
			this.sourceConjunctCandidates = sourceConjunctCandidates;
			this.sourceConjunctSizes = sourceConjunctSizes;
			this.functionalSentences = functionalSentences;
			this.functionalSentencesInfo = functionalSentencesInfo;
			this.varDomainSizes = varDomainSizes;
		}

		public List<GdlSentence> getFunctionalConjuncts() {
			//Returns, for each var, the conjunct defining it (if any)
			List<GdlSentence> functionalConjuncts = new ArrayList<GdlSentence>(functionalConjunctIndices.size());
			for(int index : functionalConjunctIndices) {
				if(index == -1)
					functionalConjuncts.add(null);
				else
					functionalConjuncts.add(functionalSentences.get(index));
			}
			return functionalConjuncts;
		}

		public List<GdlSentence> getSourceConjuncts() {
			//These are the selected source conjuncts, not just the candidates.
			List<GdlSentence> sourceConjuncts = new ArrayList<GdlSentence>(sourceConjunctIndices.size());
			for(int index : sourceConjunctIndices) {
				sourceConjuncts.add(sourceConjunctCandidates.get(index));
			}
			return sourceConjuncts;
		}

		public List<GdlVariable> getVariableOrdering() {
			return varOrdering;
		}

		/**
		 * This constructor is for "completing" the ordering by
		 * adding all remaining variables, in some arbitrary order.
		 * No source conjuncts or functions are added.
		 */
		public IterationOrderCandidate(
				IterationOrderCandidate parent) {
			//Shared rules
			this.varsToAssign = parent.varsToAssign;
			this.sourceConjunctCandidates = parent.sourceConjunctCandidates;
			this.sourceConjunctSizes = parent.sourceConjunctSizes;
			this.functionalSentences = parent.functionalSentences;
			this.functionalSentencesInfo = parent.functionalSentencesInfo;
			this.varDomainSizes = parent.varDomainSizes;

			//Individual rules:
			//We can share this because we won't be adding to it
			sourceConjunctIndices = parent.sourceConjunctIndices;
			//These others we'll be adding to
			varOrdering = new ArrayList<GdlVariable>(parent.varOrdering);
			functionalConjunctIndices = new ArrayList<Integer>(parent.functionalConjunctIndices);
			varSources = new ArrayList<Integer>(parent.varSources);
			//Fill out the ordering with all remaining variables: Easy enough
			for(GdlVariable var : varsToAssign) {
				if(!varOrdering.contains(var)) {
					varOrdering.add(var);
					functionalConjunctIndices.add(-1);
					varSources.add(-1);
				}
			}
		}

		/**
		 * This constructor is for adding a source conjunct to an
		 * ordering.
		 * @param i The index of the source conjunct being added.
		 */
		public IterationOrderCandidate(
				IterationOrderCandidate parent, int i) {
			//Shared rules:
			this.varsToAssign = parent.varsToAssign;
			this.sourceConjunctCandidates = parent.sourceConjunctCandidates;
			this.sourceConjunctSizes = parent.sourceConjunctSizes;
			this.functionalSentences = parent.functionalSentences;
			this.functionalSentencesInfo = parent.functionalSentencesInfo;
			this.varDomainSizes = parent.varDomainSizes;


			//Individual rules:
			sourceConjunctIndices = new ArrayList<Integer>(parent.sourceConjunctIndices);
			varOrdering = new ArrayList<GdlVariable>(parent.varOrdering);
			functionalConjunctIndices = new ArrayList<Integer>(parent.functionalConjunctIndices);
			varSources = new ArrayList<Integer>(parent.varSources);
			//Add the new source conjunct
			sourceConjunctIndices.add(i);
			GdlSentence sourceConjunctCandidate = sourceConjunctCandidates.get(i);
			List<GdlVariable> varsFromConjunct = GdlUtils.getVariables(sourceConjunctCandidate);
			//Ignore both previously added vars and duplicates
			//Oh, but we need to be careful here, at some point.
			//i.e., what if there are multiple of the same variable
			//in a single statement?
			//That should probably be handled later.
			for(GdlVariable var : varsFromConjunct) {
				if(!varOrdering.contains(var)) {
					varOrdering.add(var);
					varSources.add(i);
					functionalConjunctIndices.add(-1);
				}
			}
		}

		/**
		 * This constructor is for adding a function to the ordering.
		 */
		public IterationOrderCandidate(
				IterationOrderCandidate parent,
				GdlSentence functionalSentence,
				int functionalSentenceIndex, GdlVariable functionOutput) {
			//Shared rules:
			this.varsToAssign = parent.varsToAssign;
			this.sourceConjunctCandidates = parent.sourceConjunctCandidates;
			this.sourceConjunctSizes = parent.sourceConjunctSizes;
			this.functionalSentences = parent.functionalSentences;
			this.functionalSentencesInfo = parent.functionalSentencesInfo;
			this.varDomainSizes = parent.varDomainSizes;

			//Individual rules:
			sourceConjunctIndices = new ArrayList<Integer>(parent.sourceConjunctIndices);
			varOrdering = new ArrayList<GdlVariable>(parent.varOrdering);
			functionalConjunctIndices = new ArrayList<Integer>(parent.functionalConjunctIndices);
			varSources = new ArrayList<Integer>(parent.varSources);
			//And we add the function
			List<GdlVariable> varsInFunction = GdlUtils.getVariables(functionalSentence);
			//First, add the remaining arguments
			for(GdlVariable var : varsInFunction) {
				if(!varOrdering.contains(var) && !var.equals(functionOutput) && varsToAssign.contains(var)) {
					varOrdering.add(var);
					functionalConjunctIndices.add(-1);
					varSources.add(-1);
				}
			}
			//Then the output
			varOrdering.add(functionOutput);
			functionalConjunctIndices.add(functionalSentenceIndex);
			varSources.add(-1);
		}

		public long getHeuristicValue() {
			long heuristic = 1;
			for(int sourceIndex : sourceConjunctIndices) {
				heuristic *= sourceConjunctSizes.get(sourceIndex);
			}
			for(int v = 0; v < varOrdering.size(); v++) {
				if(varSources.get(v) == -1 && functionalConjunctIndices.get(v) == -1) {
					//It's not set by a source conjunct or a function
					heuristic *= varDomainSizes.get(varOrdering.get(v));
				}
			}


			//We want complete orderings to show up faster
			//so we add a little incentive to pick them
			//Add 1 to the value of non-complete orderings
			if(varOrdering.size() < varsToAssign.size())
				heuristic++;

//			System.out.println("Heuristic value is " + heuristic + " with functionalConjunctIndices " + functionalConjunctIndices);
			return heuristic;
		}
		public boolean isComplete() {
			return varOrdering.containsAll(varsToAssign);
		}
		public List<IterationOrderCandidate> getChildren(boolean analyticFunctionOrdering) {
			List<IterationOrderCandidate> allChildren = new ArrayList<IterationOrderCandidate>();
			allChildren.addAll(getSourceConjunctChildren());
			allChildren.addAll(getFunctionAddedChildren(analyticFunctionOrdering));
//			System.out.println("Number of children being added: " + allChildren.size());
			return allChildren;
		}
		private List<IterationOrderCandidate> getSourceConjunctChildren() {
			List<IterationOrderCandidate> children = new ArrayList<IterationOrderCandidate>();

			//If we are already using functions, short-circuit to cut off
			//repetition of the search space
			for(int index : functionalConjunctIndices) {
				if(index != -1) {
					return Collections.emptyList();
				}
			}

			//This means we want a reference to the original list of conjuncts.
			int lastSourceConjunctIndex = -1;
			if(!sourceConjunctIndices.isEmpty())
				lastSourceConjunctIndex = sourceConjunctIndices.get(sourceConjunctIndices.size() - 1);

			for(int i = lastSourceConjunctIndex + 1; i < sourceConjunctCandidates.size(); i++) {
				children.add(new IterationOrderCandidate(this, i));
			}
			return children;
		}
		private List<IterationOrderCandidate> getFunctionAddedChildren(boolean analyticFunctionOrdering) {
			//We can't just add those functions that
			//are "ready" to be added. We should be adding all those variables
			//"leading up to" the functions and then applying the functions.
			//We can even take this one step further by only adding one child
			//per remaining constant function; we choose as our function output the
			//variable that is a candidate for functionhood that has the
			//largest domain, or one that is tied for largest.
			//New criterion: Must also NOT be in preassignment.

			List<IterationOrderCandidate> children = new ArrayList<IterationOrderCandidate>();

			//It would be really nice here to just analytically choose
			//the set of functions we're going to use.
			//Here's one approach for doing that:
			//For each variable, get a list of the functions that could
			//potentially produce it.
			//For all the variables with no functions, add them.
			//Then repeatedly find the function with the fewest
			//number of additional variables (hopefully 0!) needed to
			//specify it and add it as a function.
			//The goal here is not to be optimal, but to be efficient!
			//Certain games (e.g. Pentago) break the old complete search method!

			//TODO: Eventual possible optimization here:
			//If something is dependent on a connected component that it is
			//not part of, wait until the connected component is resolved
			//(or something like that...)
			if(analyticFunctionOrdering && functionalSentencesInfo.size() > 8) {
				//For each variable, a list of functions
				//(refer to functions by their indices)
				//and the set of outstanding vars they depend on...
				Map<GdlVariable, Set<Integer>> functionsProducingVars = new HashMap<GdlVariable, Set<Integer>>();
				//We start by adding to the varOrdering the vars not produced by functions
				//First, we have to find them
				for(int i = 0; i < functionalSentencesInfo.size(); i++) {
					GdlSentence functionalSentence = functionalSentences.get(i);
					FunctionInfo functionInfo = functionalSentencesInfo.get(i);
					Set<GdlVariable> producibleVars = functionInfo.getProducibleVars(functionalSentence);
					for(GdlVariable producibleVar : producibleVars) {
						if(!functionsProducingVars.containsKey(producibleVar))
							functionsProducingVars.put(producibleVar, new HashSet<Integer>());
						functionsProducingVars.get(producibleVar).add(i);
					}
				}
				//Non-producible vars get iterated over before we start
				//deciding which functions to add
				for(GdlVariable var : varsToAssign) {
					if(!varOrdering.contains(var)) {
						if(!functionsProducingVars.containsKey(var)) {
							//Add var to the ordering
							varOrdering.add(var);
							functionalConjunctIndices.add(-1);
							varSources.add(-1);
						}
					}
				}


				//Map is from potential set of dependencies to function indices
				Map<Set<GdlVariable>, Set<Integer>> functionsHavingDependencies = new HashMap<Set<GdlVariable>, Set<Integer>>();
				//Create this map...
				for(int i = 0; i < functionalSentencesInfo.size(); i++) {
					GdlSentence functionalSentence = functionalSentences.get(i);
					FunctionInfo functionInfo = functionalSentencesInfo.get(i);
					Set<GdlVariable> producibleVars = functionInfo.getProducibleVars(functionalSentence);
					Set<GdlVariable> allVars = new HashSet<GdlVariable>(GdlUtils.getVariables(functionalSentence));
					//Variables already in varOrdering don't go in dependents list
					producibleVars.removeAll(varOrdering);
					allVars.removeAll(varOrdering);
					for(GdlVariable producibleVar : producibleVars) {
						Set<GdlVariable> dependencies = new HashSet<GdlVariable>();
						dependencies.addAll(allVars);
						dependencies.remove(producibleVar);
						if(!functionsHavingDependencies.containsKey(dependencies))
							functionsHavingDependencies.put(dependencies, new HashSet<Integer>());
						functionsHavingDependencies.get(dependencies).add(i);
					}
				}
				//Now, we can keep creating functions to generate the remaining variables
				while(varOrdering.size() < varsToAssign.size()) {
					if(functionsHavingDependencies.isEmpty())
						throw new RuntimeException("We should not run out of functions we could use");
					//Find the smallest set of dependencies
					Set<GdlVariable> dependencySetToUse = null;
					if(functionsHavingDependencies.containsKey(Collections.emptySet())) {
						dependencySetToUse = Collections.emptySet();
					} else {
						int smallestSize = Integer.MAX_VALUE;
						for(Set<GdlVariable> dependencySet : functionsHavingDependencies.keySet()) {
							if(dependencySet.size() < smallestSize) {
								smallestSize = dependencySet.size();
								dependencySetToUse = dependencySet;
							}
						}
					}
					//See if any of the functions are applicable
					Set<Integer> functions = functionsHavingDependencies.get(dependencySetToUse);
					int functionToUse = -1;
					GdlVariable varProduced = null;
					for (int function : functions) {
						GdlSentence functionalSentence = functionalSentences.get(function);
						FunctionInfo functionInfo = functionalSentencesInfo.get(function);
						Set<GdlVariable> producibleVars = functionInfo.getProducibleVars(functionalSentence);
						producibleVars.removeAll(dependencySetToUse);
						producibleVars.removeAll(varOrdering);
						if(!producibleVars.isEmpty()) {
							functionToUse = function;
							varProduced = producibleVars.iterator().next();
							break;
						}
					}

					if(functionToUse == -1) {
						//None of these functions were actually useful now?
						//Dump the dependency set
						functionsHavingDependencies.remove(dependencySetToUse);
					} else {
						//Apply the function
						//1) Add the remaining dependencies as iterated variables
						for(GdlVariable var : dependencySetToUse) {
							varOrdering.add(var);
							functionalConjunctIndices.add(-1);
							varSources.add(-1);
						}
						//2) Add the function's produced variable (varProduced)
						varOrdering.add(varProduced);
						functionalConjunctIndices.add(functionToUse);
						varSources.add(-1);
						//3) Remove all vars added this way from all dependency sets
						Set<GdlVariable> addedVars = new HashSet<GdlVariable>();
						addedVars.addAll(dependencySetToUse);
						addedVars.add(varProduced);
						//Tricky, because we have to merge sets
						//Easier to use a new map
						Map<Set<GdlVariable>, Set<Integer>> newFunctionsHavingDependencies = new HashMap<Set<GdlVariable>, Set<Integer>>();
						for(Entry<Set<GdlVariable>, Set<Integer>> entry : functionsHavingDependencies.entrySet()) {
							Set<GdlVariable> newKey = new HashSet<GdlVariable>(entry.getKey());
							newKey.removeAll(addedVars);
							if(!newFunctionsHavingDependencies.containsKey(newKey))
								newFunctionsHavingDependencies.put(newKey, new HashSet<Integer>());
							newFunctionsHavingDependencies.get(newKey).addAll(entry.getValue());
						}
						functionsHavingDependencies = newFunctionsHavingDependencies;
						//4) Remove this function from the lists?
						for(Set<Integer> functionSet : functionsHavingDependencies.values())
							functionSet.remove(functionToUse);
					}

				}

				//Now we need to actually return the ordering in a list
				//Here's the quick way to do that...
				//(since we've added all the new stuff to ourself already)
				return Collections.singletonList(new IterationOrderCandidate(this));

			} else {

				//Let's try a new technique for restricting the space of possibilities...
				//We already have an ordering on the functions
				//Let's try to constrain things to that order
				//Namely, if i<j and constant form j is already used as a function,
				//we cannot use constant form i UNLESS constant form j supplies
				//as its variable something used by constant form i.
				//We might also try requiring that c.f. i NOT provide a variable
				//used by c.f. j, though there may be multiple possibilities as
				//to what it could provide.
				int lastFunctionUsedIndex = -1;
				if (!functionalConjunctIndices.isEmpty()) {
					lastFunctionUsedIndex = Collections.max(functionalConjunctIndices);
				}
				Set<GdlVariable> varsProducedByFunctions = new HashSet<GdlVariable>();
				for (int i = 0; i < functionalConjunctIndices.size(); i++) {
					if (functionalConjunctIndices.get(i) != -1) {
						varsProducedByFunctions.add(varOrdering.get(i));
					}
				}
				for (int i = 0; i < functionalSentencesInfo.size(); i++) {
					GdlSentence functionalSentence = functionalSentences.get(i);
					FunctionInfo functionInfo = functionalSentencesInfo.get(i);

					if (i < lastFunctionUsedIndex) {
						//We need to figure out whether i could use any of the
						//vars we're producing with functions
						//TODO: Try this with a finer grain
						//i.e., see if i needs a var from a function that is after
						//it, not one that might be before it
						List<GdlVariable> varsInSentence = GdlUtils.getVariables(functionalSentence);
						if (Collections.disjoint(varsInSentence, varsProducedByFunctions)) {
							continue;
						}
					}

					//What is the best variable to grab from this form, if there are any?
					GdlVariable bestVariable = getBestVariable(functionalSentence, functionInfo);
					if (bestVariable == null) {
						continue;
					}
					IterationOrderCandidate newCandidate =
						new IterationOrderCandidate(this, functionalSentence, i, bestVariable);
					children.add(newCandidate);
				}

				//If there are no more functions to add, add the completed version
				if (children.isEmpty()) {
					children.add(new IterationOrderCandidate(this));
				}
				return children;
			}
		}
		private GdlVariable getBestVariable(GdlSentence functionalSentence,
				FunctionInfo functionInfo) {
			//If all the variables that can be set by the functional sentence are in
			//the varOrdering, we return null. Otherwise, we return one of
			//those with the largest domain.

			//The FunctionInfo is sentence-independent, so we need the context
			//of the sentence (which has variables in it).
			List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(functionalSentence);
			List<Boolean> dependentSlots = functionInfo.getDependentSlots();
			if(tuple.size() != dependentSlots.size())
				throw new RuntimeException("Mismatched sentence " + functionalSentence + " and constant form " + functionInfo);

			Set<GdlVariable> candidateVars = new HashSet<GdlVariable>();
			for(int i = 0; i < tuple.size(); i++) {
				GdlTerm term = tuple.get(i);
				if(term instanceof GdlVariable && dependentSlots.get(i)
						&& !varOrdering.contains(term)
						&& varsToAssign.contains(term))
					candidateVars.add((GdlVariable) term);
			}
			//TODO: Should we just generate the candidate vars with a call to getProducibleVars?
			Set<GdlVariable> producibleVars = functionInfo.getProducibleVars(functionalSentence);
			candidateVars.retainAll(producibleVars);
			//Now we look at the domains, trying to find the largest
			GdlVariable bestVar = null;
			int bestDomainSize = 0;
			for(GdlVariable var : candidateVars) {
				int domainSize = varDomainSizes.get(var);
				if(domainSize > bestDomainSize) {
					bestVar = var;
					bestDomainSize = domainSize;
				}
			}
			return bestVar; //null if none are usable
		}

		//This class has a natural ordering that is inconsistent with equals.
		@Override
		public int compareTo(IterationOrderCandidate o) {
			long diff = getHeuristicValue() - o.getHeuristicValue();
			if(diff < 0)
				return -1;
			else if(diff == 0)
				return 0;
			else
				return 1;
		}
		@Override
		public String toString() {
			return varOrdering.toString() + " with sources " + getSourceConjuncts().toString() + "; functional?: " + functionalConjunctIndices + "; domain sizes are " + this.varDomainSizes;
		}
	}