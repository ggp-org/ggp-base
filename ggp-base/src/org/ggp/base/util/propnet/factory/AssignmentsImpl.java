/**
 *
 */
package org.ggp.base.util.propnet.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormSource;
import org.ggp.base.util.gdl.model.SentenceModel;
import org.ggp.base.util.gdl.transforms.CommonTransforms;
import org.ggp.base.util.gdl.transforms.ConstantFinder.ConstantChecker;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Constant;


public class AssignmentsImpl implements Assignments {
	private boolean empty;
	private boolean allDone = false;
	//Contains all the assignments of variables we could make
	private Map<GdlVariable, GdlConstant> headAssignment;

	private List<GdlVariable> varsToAssign;
	private List<List<GdlConstant>> valuesToIterate;
	private List<AssignmentFunction> valuesToCompute;
	private List<Integer> indicesToChangeWhenNull; //See note below
	private List<GdlDistinct> distincts;
	private List<GdlVariable> varsToChangePerDistinct; //indexing same as distincts

	/*
	 * What does indicesToChangeWhenNull do? Well, sometimes after incrementing
	 * part of the iterator, we find that a function being used to define a slot
	 * in the tuple has no value corresponding to its inputs (the inputs are
	 * outside the function's domain). In that case, we set the value to null,
	 * then leave it to the makeNextAssignmentValid() method to deal with it.
	 * We want to increment something in the input, but we need to know what
	 * in the input we should increment (i.e. which is the rightmost slot in
	 * the function's input). This is recorded in indicesToChangeWhenNull. If
	 * a slot is not defined by a function, then presumably it will not be null,
	 * so its value here is unimportant. Setting its value to -1 would help
	 * catch errors.
	 */

	private List<List<List<GdlConstant>>> tuplesBySource; //indexed by conjunct
	private List<Integer> sourceDefiningSlot; //indexed by var slot
	private List<List<Integer>> varsChosenBySource; //indexed by conjunct, then slot
	private List<List<Boolean>> putDontCheckBySource; //indexed by conjunct, then slot

	/**
	 * Creates an Assignments object that generates AssignmentIterators.
	 * These can be used to efficiently iterate over all possible assignments
	 * for variables in a given rule.
	 *
	 * @param headAssignment An assignment of variables whose values should be
	 * fixed. May be empty.
	 * @param rule The rule whose assignments we want to iterate over.
	 * @param varDomains A map containing the possible values for each variable
	 * in the rule. (All such values are GdlConstants.)
	 * @param constantForms
	 * @param completedSentenceFormValues
	 * @param sentenceFormSource
	 */
	public AssignmentsImpl(Map<GdlVariable, GdlConstant> headAssignment,
			GdlRule rule, Map<GdlVariable, Set<GdlConstant>> varDomains,
			Map<SentenceForm, ConstantForm> constantForms,
			Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues,
					SentenceFormSource sentenceFormSource) {
		empty = false;
		this.headAssignment = headAssignment;

		//We first have to find the remaining variables in the body
		varsToAssign = GdlUtils.getVariables(rule);
		//Remove all the duplicates; we do, however, want to keep the ordering
		List<GdlVariable> newVarsToAssign = new ArrayList<GdlVariable>();
		for(GdlVariable v : varsToAssign)
			if(!newVarsToAssign.contains(v))
				newVarsToAssign.add(v);
		varsToAssign = newVarsToAssign;
		varsToAssign.removeAll(headAssignment.keySet());
		//varsToAssign is set at this point

		//We see if iterating over entire tuples will give us a
		//better result, and we look for the best way of doing that.

		//Let's get the domains of the variables
		//Map<GdlVariable, Set<GdlConstant>> varDomains = model.getVarDomains(rule);
		//Since we're looking at a particular rule, we can do this one step better
		//by looking at the domain of the head, which may be more restrictive
		//and taking the intersections of the two domains where applicable
		//Map<GdlVariable, Set<GdlConstant>> headVarDomains = model.getVarDomainsInSentence(rule.getHead());

		//We can run the A* search for a good set of source conjuncts
		//at this point, then use the result to build the rest.
		Map<SentenceForm, Integer> completedSentenceFormSizes = new HashMap<SentenceForm, Integer>();
		if(completedSentenceFormValues != null)
			for(SentenceForm form : completedSentenceFormValues.keySet())
				completedSentenceFormSizes.put(form, completedSentenceFormValues.get(form).size());
		Map<GdlVariable, Integer> varDomainSizes = new HashMap<GdlVariable, Integer>();
		for(GdlVariable var : varDomains.keySet())
			varDomainSizes.put(var, varDomains.get(var).size());

		IterationOrderCandidate bestOrdering;
		bestOrdering = getBestIterationOrderCandidate(rule, varDomains,/*model,*/ constantForms, completedSentenceFormSizes, headAssignment, false, sentenceFormSource); //TODO: True here?

		//Want to replace next few things with order
		//Need a few extra things to handle the use of iteration over existing tuples
		varsToAssign = bestOrdering.getVariableOrdering();

		//For each of these vars, we have to find one or the other.
		//Let's start by finding all the domains, a task already done.
		valuesToIterate = new ArrayList<List<GdlConstant>>(varsToAssign.size());

		for(GdlVariable var : varsToAssign) {
			if(varDomains.containsKey(var)) {
				if(!varDomains.get(var).isEmpty())
					valuesToIterate.add(new ArrayList<GdlConstant>(varDomains.get(var)));
				else
					valuesToIterate.add(Collections.singletonList(GdlPool.getConstant("0")));
			} else {
				valuesToIterate.add(Collections.singletonList(GdlPool.getConstant("0")));
			}
		}
		//Okay, the iteration-over-domain is done.
		//Now let's look at sourced iteration.
		sourceDefiningSlot = new ArrayList<Integer>(varsToAssign.size());
		for(int i = 0; i < varsToAssign.size(); i++) {
			sourceDefiningSlot.add(-1);
		}

		//We also need to convert values into tuples
		//We should do so while constraining to any constants in the conjunct
		//Let's convert the conjuncts
		List<GdlSentence> sourceConjuncts = bestOrdering.getSourceConjuncts();
		tuplesBySource = new ArrayList<List<List<GdlConstant>>>(sourceConjuncts.size());
		varsChosenBySource = new ArrayList<List<Integer>>(sourceConjuncts.size());
		putDontCheckBySource = new ArrayList<List<Boolean>>(sourceConjuncts.size());
		for(int j = 0; j < sourceConjuncts.size(); j++) {
			GdlSentence sourceConjunct = sourceConjuncts.get(j);
			SentenceForm form = sentenceFormSource.getSentenceForm(sourceConjunct);
			//flatten into a tuple
			List<GdlTerm> conjunctTuple = GdlUtils.getTupleFromSentence(sourceConjunct);
			//Go through the vars/constants in the tuple
			List<Integer> constraintSlots = new ArrayList<Integer>();
			List<GdlConstant> constraintValues = new ArrayList<GdlConstant>();
			List<Integer> varsChosen = new ArrayList<Integer>();
			List<Boolean> putDontCheck = new ArrayList<Boolean>();
			for(int i = 0; i < conjunctTuple.size(); i++) {
				GdlTerm term = conjunctTuple.get(i);
				if(term instanceof GdlConstant) {
					constraintSlots.add(i);
					constraintValues.add((GdlConstant) term);
					//TODO: What if tuple size ends up being 0?
					//Need to keep that in mind
				} else if(term instanceof GdlVariable) {
					int varIndex = varsToAssign.indexOf(term);
					varsChosen.add(varIndex);
					if(sourceDefiningSlot.get(varIndex) == -1) {
						//We define it
						sourceDefiningSlot.set(varIndex, j);
						putDontCheck.add(true);
					} else {
						//It's an overlap; we just check for consistency
						putDontCheck.add(false);
					}
				} else {
					throw new RuntimeException("Function returned in tuple");
				}
			}
			varsChosenBySource.add(varsChosen);
			putDontCheckBySource.add(putDontCheck);

			//Now we put the tuples together
			//We use constraintSlots and constraintValues to check that the
			//tuples have compatible values
			Collection<GdlSentence> sentences = completedSentenceFormValues.get(form);
			List<List<GdlConstant>> tuples = new ArrayList<List<GdlConstant>>();
			byTuple: for(GdlSentence sentence : sentences) {
				List<GdlConstant> longTuple = GdlUtils.getTupleFromGroundSentence(sentence);
				List<GdlConstant> shortTuple = new ArrayList<GdlConstant>(varsChosen.size());
				for(int c = 0; c < constraintSlots.size(); c++) {
					int slot = constraintSlots.get(c);
					GdlConstant value = constraintValues.get(c);
					if(!longTuple.get(slot).equals(value))
						continue byTuple;
				}
				int c = 0;
				for(int s = 0; s < longTuple.size(); s++) {
					//constraintSlots is sorted in ascending order
					if(c < constraintSlots.size()
							&& constraintSlots.get(c) == s)
						c++;
					else
						shortTuple.add(longTuple.get(s));
				}
				//The tuple fits the source conjunct
				tuples.add(shortTuple);
			}
			//sortTuples(tuples); //Needed? Useful? Not sure. Probably not?
			tuplesBySource.add(tuples);
		}


		//We now want to see which we can give assignment functions to
		valuesToCompute = new ArrayList<AssignmentFunction>(varsToAssign.size());
		for(@SuppressWarnings("unused") GdlVariable var : varsToAssign) {
			valuesToCompute.add(null);
		}
		indicesToChangeWhenNull = new ArrayList<Integer>(varsToAssign.size());
		for(int i = 0; i < varsToAssign.size(); i++) {
			//Change itself, why not?
			//Actually, instead let's try -1, to catch bugs better
			indicesToChangeWhenNull.add(-1);
		}
		//Now we have our functions already selected by the ordering
		//bestOrdering.functionalConjunctIndices;

		//Make AssignmentFunctions out of the ordering
		List<GdlSentence> functionalConjuncts = bestOrdering.getFunctionalConjuncts();
		for(int i = 0; i < functionalConjuncts.size(); i++) {
			GdlSentence functionalConjunct = functionalConjuncts.get(i);
			if(functionalConjunct != null) {
				//These are the only ones that could be constant functions
				SentenceForm conjForm = sentenceFormSource.getSentenceForm(functionalConjunct);
				ConstantForm constForm = null;

				if(constantForms != null)
					constForm = constantForms.get(conjForm);
				if(constForm != null) {
					//Now we need to figure out which variables are involved
					//and which are suitable as functional outputs.

					//1) Which vars are in this conjunct?
					List<GdlVariable> varsInSentence = GdlUtils.getVariables(functionalConjunct);
					//2) Of these vars, which is "rightmost"?
					GdlVariable rightmostVar = getRightmostVar(varsInSentence);
					//3) Is it only used once in the relation?
					if(Collections.frequency(varsInSentence, rightmostVar) != 1)
						continue; //Can't use it
					//4) Which slot is it used in in the relation?
					//5) Build an AssignmentFunction if appropriate.
					//   This should be able to translate from values of
					//   the other variables to the value of the wanted
					//   variable.
					AssignmentFunction function = new AssignmentFunction((GdlRelation)functionalConjunct, constForm, rightmostVar, varsToAssign, headAssignment);
					//We don't guarantee that this works until we check
					if(!function.functional())
						continue;
					int index = varsToAssign.indexOf(rightmostVar);
					valuesToCompute.set(index, function);
					Set<GdlVariable> remainingVarsInSentence = new HashSet<GdlVariable>(varsInSentence);
					remainingVarsInSentence.remove(rightmostVar);
					GdlVariable nextRightmostVar = getRightmostVar(remainingVarsInSentence);
					indicesToChangeWhenNull.set(index, varsToAssign.indexOf(nextRightmostVar));
				}
			}
		}

		//We now have the remainingVars also assigned their domains
		//We also cover the distincts here
		//Assume these are just variables and constants
		distincts = new ArrayList<GdlDistinct>();
		for(GdlLiteral literal : rule.getBody()) {
			if(literal instanceof GdlDistinct)
				distincts.add((GdlDistinct) literal);
		}

		computeVarsToChangePerDistinct();


		//Need to add "distinct" restrictions to head assignment, too...
		checkDistinctsAgainstHead();

		//We are ready for iteration
	}

	private GdlVariable getRightmostVar(Collection<GdlVariable> vars) {
		GdlVariable rightmostVar = null;
		for(GdlVariable var : varsToAssign)
			if(vars.contains(var))
				rightmostVar = var;
		return rightmostVar;
	}

	public AssignmentsImpl() {
		//The assignment is impossible; return nothing
		empty = true;
	}
	@SuppressWarnings("unchecked")
    public AssignmentsImpl(GdlRule rule, /*SentenceModel model,*/ Map<GdlVariable, Set<GdlConstant>> varDomains,
			Map<SentenceForm, ConstantForm> constantForms,
			Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues,
					SentenceFormSource sentenceFormSource) {
		this(Collections.EMPTY_MAP, rule, varDomains, constantForms, completedSentenceFormValues, sentenceFormSource);
	}

	private void checkDistinctsAgainstHead() {
		for(GdlDistinct distinct : distincts) {
			GdlTerm term1 = CommonTransforms.replaceVariables(distinct.getArg1(), headAssignment);
			GdlTerm term2 = CommonTransforms.replaceVariables(distinct.getArg2(), headAssignment);
			if(term1.equals(term2)) {
				//This fails
				empty = true;
				allDone = true;
			}
		}
	}
	@Override
	public Iterator<Map<GdlVariable, GdlConstant>> iterator() {
		return new AssignmentIteratorImpl();
	}
	@Override
	public AssignmentIterator getIterator() {
		return new AssignmentIteratorImpl();
	}

	private void computeVarsToChangePerDistinct() {
		//remember that iterators must be set up first
		varsToChangePerDistinct = new ArrayList<GdlVariable>(varsToAssign.size());
		for(GdlDistinct distinct : distincts) {
			//For two vars, we want to record the later of the two
			//For one var, we want to record the one
			//For no vars, we just put null
			List<GdlVariable> varsInDistinct = new ArrayList<GdlVariable>(2);
			if(distinct.getArg1() instanceof GdlVariable)
				varsInDistinct.add((GdlVariable) distinct.getArg1());
			if(distinct.getArg2() instanceof GdlVariable)
				varsInDistinct.add((GdlVariable) distinct.getArg2());

			GdlVariable varToChange = null;
			if(varsInDistinct.size() == 1) {
				varToChange = varsInDistinct.get(0);
			} else if(varsInDistinct.size() == 2) {
				varToChange = getRightmostVar(varsInDistinct);
			}
			varsToChangePerDistinct.add(varToChange);
		}
	}

	public class AssignmentIteratorImpl implements AssignmentIterator {
		List<Integer> sourceTupleIndices = null;
		//This time we just have integers to deal with
		List<Integer> valueIndices = null;
		List<GdlConstant> nextAssignment = new ArrayList<GdlConstant>();
		Map<GdlVariable, GdlConstant> assignmentMap = new HashMap<GdlVariable, GdlConstant>();

		boolean headOnly = false;
		boolean done = false;

		public AssignmentIteratorImpl() {
			if(varsToAssign == null) {
				headOnly = true;
				return;
			}

			//Set up source tuple...
			sourceTupleIndices = new ArrayList<Integer>(tuplesBySource.size());
			for(int i = 0; i < tuplesBySource.size(); i++) {
				sourceTupleIndices.add(0);
			}
			//Set up...
			valueIndices = new ArrayList<Integer>(varsToAssign.size());
			for(int i = 0; i < varsToAssign.size(); i++) {
				valueIndices.add(0);
				nextAssignment.add(null);
			}

			assignmentMap.putAll(headAssignment);

			//Update "nextAssignment" according to the values of the
			//value indices
			updateNextAssignment();
			//Keep updating it until something really works
			makeNextAssignmentValid();
		}


		private void makeNextAssignmentValid() {
			if(nextAssignment == null)
				return;

			//Something new that can pop up with functional constants...
			for(int i = 0; i < nextAssignment.size(); i++) {
				if(nextAssignment.get(i) == null) {
					//Some function doesn't agree with the answer here
					//So what do we increment?
					incrementIndex(indicesToChangeWhenNull.get(i));
					if(nextAssignment == null)
						return;
					i = -1;
				}
			}

			//Find all the unsatisfied distincts
			//Find the pair with the earliest var. that needs to be changed
			List<GdlVariable> varsToChange = new ArrayList<GdlVariable>();
			for(int d = 0; d < distincts.size(); d++) {
				GdlDistinct distinct = distincts.get(d);
				//The assignments must use the assignments implied by nextAssignment
				GdlConstant term1 = replaceVariables(distinct.getArg1());
				GdlConstant term2 = replaceVariables(distinct.getArg2());
				if(term1.equals(term2)) {
					//need to change one of these
					varsToChange.add(varsToChangePerDistinct.get(d));
				}
			}
			if(!varsToChange.isEmpty()) {
				GdlVariable varToChange = getLeftmostVar(varsToChange);
				//We want just the one, as it is a full restriction on its
				//own behalf
				changeOneInNext(Collections.singleton(varToChange));
			}

		}

		private GdlVariable getLeftmostVar(List<GdlVariable> vars) {
			for(GdlVariable var : varsToAssign)
				if(vars.contains(var))
					return var;
			return null;
		}

		private GdlConstant replaceVariables(GdlTerm term) {
			if(term instanceof GdlFunction)
				throw new RuntimeException("Function in the distinct... not handled");
			//Use the assignments implied by nextAssignment
			if(headAssignment.containsKey(term))
				return headAssignment.get(term); //Translated in head assignment
			if(term instanceof GdlConstant)
				return (GdlConstant) term;
			int index = varsToAssign.indexOf(term);
			return nextAssignment.get(index);
		}

		private void incrementIndex(int index) {
			if(index < 0) {
				//Trash the iterator
				nextAssignment = null;
				return;
			}
			if(valuesToCompute != null && valuesToCompute.get(index) != null) {
				//The constant at this index is functionally computed
				incrementIndex(index - 1);
				return;
			}
			if(sourceDefiningSlot.get(index) != -1) {
				//This is set by a source; increment the source
				incrementSource(sourceDefiningSlot.get(index));
				return;
			}
			//We try increasing the var at index by 1.
			//Everything to the right of it gets reset.
			//If it can't be increased, increase the number
			//to the left instead. If nothing can be
			//increased, trash the iterator.
			int curValue = valueIndices.get(index);
			if(curValue == valuesToIterate.get(index).size() - 1) {
				//We have no room to increase the value
				incrementIndex(index - 1);
				return;
			}
			//Increment the current value
			valueIndices.set(index, curValue + 1);
			//Reset everything to the right of the current value
			for(int i = index + 1; i < valueIndices.size(); i++)
				valueIndices.set(i, 0);

			//Update the assignment
			updateNextAssignment();
		}

		private void incrementSource(int source) {
			if(source < 0) {
				//Trash the iterator
				nextAssignment = null;
				return;
			}

			//If we can't increase this source, increase the one to the left instead
			int curValue = sourceTupleIndices.get(source);
			if(curValue == tuplesBySource.get(source).size() - 1) {
				incrementSource(source - 1);
				return;
			}
			//Increment the current source
			sourceTupleIndices.set(source, curValue + 1);
			//Reset all the sources to the right of it
			for(int i = source + 1; i < sourceTupleIndices.size(); i++)
				sourceTupleIndices.set(i, 0);
			//Reset all the values set by iteration over domains
			for(int i = 0; i < valueIndices.size(); i++)
				valueIndices.set(i, 0);

			//Update the assignment
			updateNextAssignment();
		}


		private void updateNextAssignment() {
			//Let's set according to the sources before we get to the remainder
			for(int s = 0; s < sourceTupleIndices.size(); s++) {
				List<List<GdlConstant>> tuples = tuplesBySource.get(s);
				int curIndex = sourceTupleIndices.get(s);
				if(tuples.size() == 0) {
					System.out.println("number of sources: " + sourceTupleIndices.size());
					//Might we have to do this on occasion?
					nextAssignment = null;
					return;
				}
				List<GdlConstant> tuple = tuples.get(curIndex);
				List<Integer> varsChosen = varsChosenBySource.get(s);
				List<Boolean> putDontCheckTuple = putDontCheckBySource.get(s);
				for(int i = 0; i < tuple.size(); i++) {
					GdlConstant value = tuple.get(i);
					boolean putDontCheck = putDontCheckTuple.get(i);
					int varSlotChosen = varsChosen.get(i);
					if(putDontCheck) {
						nextAssignment.set(varSlotChosen, value);
					} else {
						//It's only at this point that we get to check...
						if(!nextAssignment.get(varSlotChosen).equals(value)) {
							//We need to correct the value
							//This is wrong! The current tuple may be the constraining tuple.
							//But we might need it for performance reasons when there isn't that case...
							//TODO: Restore this when we can tell it's appropriate
							//incrementSourceToGetValueInSlot(s, nextAssignment.get(varSlotChosen), i);
							incrementSource(s);
							//updateNextAssignment(); (should be included at end of calling function)
							return;
						}
					}
				}
			}

			for(int i = 0; i < valueIndices.size(); i++) {
				if((valuesToCompute == null || valuesToCompute.get(i) == null)
						&& sourceDefiningSlot.get(i) == -1) {
					nextAssignment.set(i, valuesToIterate.get(i).get(valueIndices.get(i)));
				} else if(sourceDefiningSlot.get(i) == -1) {
					//Fill in based on a function
					//Note that the values on the left must already be filled in
					nextAssignment.set(i, valuesToCompute.get(i).getValue(nextAssignment));
				}
			}
		}

		public void changeOneInNext(Collection<GdlVariable> vars) {
			//Basically, we want to increment the rightmost one...
			//Corner cases:
			if(nextAssignment == null)
				return;
			if(vars.isEmpty()) {
				if(headOnly) {
					done = true;
					return;
				} else {
					//Something currently constant is false
					//The assignment is done
					done = true;
					return;
				}
			}
			if(varsToAssign == null)
				System.out.println("headOnly: " + headOnly);

			GdlVariable rightmostVar = getRightmostVar(vars);
			incrementIndex(varsToAssign.indexOf(rightmostVar));
			makeNextAssignmentValid();

		}
		
		@Override
		public void changeOneInNext(Collection<GdlVariable> varsToChange,
				Map<GdlVariable, GdlConstant> assignment) {
			if(nextAssignment == null)
				return;

			//First, we stop and see if any of these have already been
			//changed (in nextAssignment)
			for(GdlVariable varToChange : varsToChange) {
				int index = varsToAssign.indexOf(varToChange);
				if(index != -1) {
					GdlConstant assignedValue = assignment.get(varToChange);
					if(assignedValue == null) {
						System.out.println("assignedValue is null");
						System.out.println("varToChange is " + varToChange);
						System.out.println("assignment is " + assignment);
					}
					if(nextAssignment == null)
						System.out.println("nextAssignment is null");
					if(!assignedValue.equals(nextAssignment.get(index)))
						//We've already changed one of these
						return;
				}
			}

			//Okay, we actually need to change one of these
			changeOneInNext(varsToChange);
		}


		@Override
		public boolean hasNext() {
			if(empty)
				return false;
			if(headOnly)
				return (!allDone && !done);

			return (nextAssignment != null);
		}

		@Override
		public Map<GdlVariable, GdlConstant> next() {
			if(headOnly) {
				if(allDone || done)
					throw new RuntimeException("Asking for next when all done");
				done = true;
				return headAssignment;
			}

			updateMap(); //Sets assignmentMap

			//Adds one to the nextAssignment
			incrementIndex(valueIndices.size() - 1);
			makeNextAssignmentValid();

			return assignmentMap;
		}

		private void updateMap() {
			//Sets the map to match the nextAssignment
			for(int i = 0; i < varsToAssign.size(); i++) {
				assignmentMap.put(varsToAssign.get(i), nextAssignment.get(i));
			}
		}

		@Override
		public void remove() {
			//Not implemented
		}
	}

	public static Assignments getAssignmentsProducingSentence(
			GdlRule rule, GdlSentence sentence, /*SentenceModel model,*/ Map<GdlVariable, Set<GdlConstant>> varDomains,
			Map<SentenceForm, ConstantForm> constantForms,
			Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues,
					SentenceFormSource sentenceFormSource) {
		//First, we see which variables must be set according to the rule head
		//(and see if there's any contradiction)
		Map<GdlVariable, GdlConstant> headAssignment = new HashMap<GdlVariable, GdlConstant>();
		if(!setVariablesInHead(rule.getHead(), sentence, headAssignment)) {
			return new AssignmentsImpl();//Collections.emptySet();
		}
		//Then we come up with all the assignments of the rest of the variables

		//We need to look for functions we can make use of

		return new AssignmentsImpl(headAssignment, rule, varDomains, constantForms, completedSentenceFormValues, sentenceFormSource);
	}

	//returns true if all variables were set successfully
	private static boolean setVariablesInHead(GdlSentence head,
			GdlSentence sentence, Map<GdlVariable, GdlConstant> assignment) {
		if(head instanceof GdlProposition)
			return true;
		return setVariablesInHead(head.getBody(), sentence.getBody(), assignment);
	}
	private static boolean setVariablesInHead(List<GdlTerm> head,
			List<GdlTerm> sentence, Map<GdlVariable, GdlConstant> assignment) {
		for(int i = 0; i < head.size(); i++) {
			GdlTerm headTerm = head.get(i);
			GdlTerm refTerm = sentence.get(i);
			if(headTerm instanceof GdlConstant) {
				if(!refTerm.equals(headTerm))
					//The rule can't produce this sentence
					return false;
			} else if(headTerm instanceof GdlVariable) {
				GdlVariable var = (GdlVariable) headTerm;
				GdlConstant curValue = assignment.get(var);
				if(curValue != null && !curValue.equals(refTerm)) {
					//inconsistent assignment (e.g. head is (rel ?x ?x), sentence is (rel 1 2))
					return false;
				}
				assignment.put(var, (GdlConstant)refTerm);
			} else if(headTerm instanceof GdlFunction) {
				//Recurse on the body
				GdlFunction headFunction = (GdlFunction) headTerm;
				GdlFunction refFunction = (GdlFunction) refTerm;
				if(!setVariablesInHead(headFunction.getBody(), refFunction.getBody(), assignment))
					return false;
			}
		}
		return true;
	}


	//Represents information about a sentence form that is constant.
	public static class ConstantForm {
		private SentenceForm form;

		private int numSlots;
		//True iff the slot has at most one value given the other slots' values
		private List<Boolean> dependentSlots = new ArrayList<Boolean>();
		private List<Map<List<GdlConstant>, GdlConstant>> valueMaps = new ArrayList<Map<List<GdlConstant>, GdlConstant>>();;

		public ConstantForm(SentenceForm form, Map<GdlSentence, Component> values, SentenceModel model) {
			this.form = form;

			numSlots = form.getTupleSize();

			List<List<GdlConstant>> tuples = getTrueTuples(form, values, model);

			for(int i = 0; i < numSlots; i++) {
				//We want to establish whether or not this is a constant...
				Map<List<GdlConstant>, GdlConstant> functionMap = new HashMap<List<GdlConstant>, GdlConstant>();
				boolean functional = true;
				for(List<GdlConstant> tuple : tuples) {
					List<GdlConstant> tuplePart = new ArrayList<GdlConstant>(tuple.size() - 1);
					tuplePart.addAll(tuple.subList(0, i));
					tuplePart.addAll(tuple.subList(i + 1, tuple.size()));
					if(functionMap.containsKey(tuplePart)) {
						//We have two tuples with different values in just this slot
						functional = false;
						break;
					}
					//Otherwise, we record it
					functionMap.put(tuplePart, tuple.get(i));
				}

				if(functional) {
					//Record the function
					dependentSlots.add(true);
					valueMaps.add(functionMap);
				} else {
					//Forget it
					dependentSlots.add(false);
					valueMaps.add(null);
				}
			}
		}

		public ConstantForm(SentenceForm form, ConstantChecker constantChecker) throws InterruptedException {
			this.form = form;

			numSlots = form.getTupleSize();

			for(int i = 0; i < numSlots; i++) {
				//We want to establish whether or not this is a constant...
				Map<List<GdlConstant>, GdlConstant> functionMap = new HashMap<List<GdlConstant>, GdlConstant>();
				boolean functional = true;
				Iterator<List<GdlConstant>> itr = constantChecker.getTrueTuples(form);
				while(itr.hasNext()) {
					ConcurrencyUtils.checkForInterruption();
					List<GdlConstant> tuple = itr.next();
					List<GdlConstant> tuplePart = new ArrayList<GdlConstant>(tuple.size() - 1);
					tuplePart.addAll(tuple.subList(0, i));
					tuplePart.addAll(tuple.subList(i + 1, tuple.size()));
					if(functionMap.containsKey(tuplePart)) {
						//We have two tuples with different values in just this slot
						functional = false;
						break;
					}
					//Otherwise, we record it
					functionMap.put(tuplePart, tuple.get(i));
				}

				if(functional) {
					//Record the function
					dependentSlots.add(true);
					valueMaps.add(functionMap);
				} else {
					//Forget it
					dependentSlots.add(false);
					valueMaps.add(null);
				}
			}
		}


		private List<List<GdlConstant>> getTrueTuples(SentenceForm form,
				Map<GdlSentence, Component> values, SentenceModel model) {
			List<List<GdlConstant>> tuples = new ArrayList<List<GdlConstant>>();
			//Working on the assumption that this will be faster than
			//going through all the keys in "values". I suppose I could
			//actually CHECK that this is the case, if it becomes an issue.
			for(GdlSentence sentence : model.getSentenceIterable(form)) {
				if(values.containsKey(sentence)) {
					Component value = values.get(sentence);
					if(value instanceof Constant && value.getValue()) {
						//It's a true constant
						//Add the tuple
						tuples.add(GdlUtils.getTupleFromGroundSentence(sentence));
					}
				}
			}
			return tuples;
		}

		public Map<List<GdlConstant>, GdlConstant> getValueMap(int index) {
			return valueMaps.get(index);
		}

		public List<Boolean> getDependentSlots() {
			return dependentSlots;
		}

		/**
		 * Given a sentence of the constant form's sentence form, finds all
		 * the variables in the sentence that can be produced functionally.
		 *
		 * Note the corner case: If a variable appears twice in a sentence,
		 * it CANNOT be produced in this way.
		 */
		public Set<GdlVariable> getProducibleVars(GdlSentence sentence) {
			if(!form.matches(sentence))
				throw new RuntimeException("Sentence "+sentence+" does not match constant form");
			List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);

			Set<GdlVariable> candidateVars = new HashSet<GdlVariable>();
			//Variables that appear multiple times go into multipleVars
			Set<GdlVariable> multipleVars = new HashSet<GdlVariable>();
			//...which, of course, means we have to spot non-candidate vars
			Set<GdlVariable> nonCandidateVars = new HashSet<GdlVariable>();

			for(int i = 0; i < tuple.size(); i++) {
				GdlTerm term = tuple.get(i);
				if(term instanceof GdlVariable
						&& !multipleVars.contains(term)) {
					GdlVariable var = (GdlVariable) term;
					if(candidateVars.contains(var)
							|| nonCandidateVars.contains(var)) {
						multipleVars.add(var);
						candidateVars.remove(var);
					} else if(dependentSlots.get(i)) {
						candidateVars.add(var);
					} else {
						nonCandidateVars.add(var);
					}
				}
			}

			return candidateVars;

		}

	}

	static class AssignmentFunction {
		//How is the AssignmentFunction going to operate?
		//Well, some of the variables are going to be
		//specified as having one or more of these functions
		//apply to them. (If multiple apply, they all have
		//to agree.)
		//We pass in the current value of the tuple and
		//it gives us the value desired (or null).
		//This means it just has to know which indices in
		//the tuple (i.e. which variables) correspond to
		//which slots in its native tuple.

		//Used when multiple assignment functions are relevant
		//to the same variable. In this case we call these other
		//functions with the same arguments and return null if
		//any of the answers differ.
		List<AssignmentFunction> internalFunctions;
		int querySize;
		List<Boolean> isInputConstant;
		List<GdlConstant> queryConstants;
		List<Integer> queryInputIndices;
		Map<List<GdlConstant>, GdlConstant> function = null;
		//Some sort of trie might work better here...

		public AssignmentFunction(GdlRelation conjunct, ConstantForm constForm,
				GdlVariable rightmostVar, List<GdlVariable> varOrder,
				Map<GdlVariable, GdlConstant> preassignment) {
			//We have to set up the things mentioned above...
			internalFunctions = new ArrayList<AssignmentFunction>();

			//We can traverse the conjunct for the list of variables/constants...
			List<GdlTerm> terms = new ArrayList<GdlTerm>();
			gatherVars(conjunct.getBody(), terms);
			//Note that we assume here that the var of interest only
			//appears once in the relation...
			int varIndex = terms.indexOf(rightmostVar);
			if(varIndex == -1) {
				System.out.println("conjunct is: " + conjunct);
				System.out.println("terms are: " + terms);
				System.out.println("righmostVar is: " + rightmostVar);
			}
			terms.remove(rightmostVar);
			function = constForm.getValueMap(varIndex);

			//Set up inputs and such, using terms
			querySize = terms.size();
			isInputConstant = new ArrayList<Boolean>(terms.size());
			queryConstants = new ArrayList<GdlConstant>(terms.size());
			queryInputIndices = new ArrayList<Integer>(terms.size());
			for(GdlTerm term : terms) {
				if(term instanceof GdlConstant) {
					isInputConstant.add(true);
					queryConstants.add((GdlConstant) term);
					queryInputIndices.add(-1);
				} else if(term instanceof GdlVariable) {
					//Is it in the head assignment?
					if(preassignment.containsKey(term)) {
						isInputConstant.add(true);
						queryConstants.add(preassignment.get(term));
						queryInputIndices.add(-1);
					} else {
						isInputConstant.add(false);
						queryConstants.add(null);
						//What value do we put here?
						//We want to grab some value out of the
						//input tuple, which uses functional ordering
						//Index of the relevant variable, by the
						//assignment's ordering
						queryInputIndices.add(varOrder.indexOf(term));
					}
				}
			}
		}

		public boolean functional() {
			return (function != null);
		}

		private void gatherVars(List<GdlTerm> body, List<GdlTerm> terms) {
			for(GdlTerm term : body) {
				if(term instanceof GdlConstant || term instanceof GdlVariable)
					terms.add(term);
				else if(term instanceof GdlFunction)
					gatherVars(((GdlFunction)term).getBody(), terms);
			}
		}

		public GdlConstant getValue(List<GdlConstant> remainingTuple) {
			//We have a map from a tuple of GdlConstants
			//to the GdlConstant we need, provided by the ConstantForm.
			//We need to make the tuple for this map.
			List<GdlConstant> queryTuple = new ArrayList<GdlConstant>(querySize);
			//Now we have to fill in the query
			for(int i = 0; i < querySize; i++) {
				if(isInputConstant.get(i)) {
					queryTuple.add(queryConstants.get(i));
				} else {
					queryTuple.add(remainingTuple.get(queryInputIndices.get(i)));
				}
			}
			//The query is filled; we ask the map
			GdlConstant answer = function.get(queryTuple);

			for(AssignmentFunction internalFunction : internalFunctions) {
				if(internalFunction.getValue(remainingTuple) != answer)
					return null;
			}
			return answer;
		}
	}

	/**
	 * Finds the iteration order (including variables, functions, and
	 * source conjuncts) that is expected to result in the fastest iteration.
	 *
	 * The value that is compared for each ordering is the product of:
	 * - For each source conjunct, the number of tuples offered by the conjunct;
	 * - For each variable not defined by a function, the size of its domain.
	 *
	 * @param constantForms
	 * @param completedSentenceFormSizes For each sentence form, this may optionally
	 * contain the number of possible sentences of this form. This is useful if the
	 * number of sentences is much lower than the product of its variables' domain
	 * sizes; however, if this contains sentence forms where the set of sentences
	 * is unknown, then it may return an ordering that is unusable.
	 */
	protected static IterationOrderCandidate getBestIterationOrderCandidate(GdlRule rule,
			/*SentenceModel model,*/
			Map<GdlVariable, Set<GdlConstant>> varDomains,
			Map<SentenceForm, ConstantForm> constantForms,
			Map<SentenceForm, Integer> completedSentenceFormSizes,
			Map<GdlVariable, GdlConstant> preassignment,
			boolean analyticFunctionOrdering,
			SentenceFormSource sentenceFormSource) {
		//Here are the things we need to pass into the first IOC constructor
		List<GdlSentence> sourceConjunctCandidates = new ArrayList<GdlSentence>();
		//What is a source conjunct candidate?
		//- It is a positive conjunct in the rule (i.e. a GdlSentence in the body).
		//- It has already been fully defined; i.e. it is not recursively defined in terms of the current form.
		//Furthermore, we know the number of potentially true tuples in it.
		List<GdlVariable> varsToAssign = GdlUtils.getVariables(rule);
		List<GdlVariable> newVarsToAssign = new ArrayList<GdlVariable>();
		for(GdlVariable var : varsToAssign)
			if(!newVarsToAssign.contains(var))
				newVarsToAssign.add(var);
		varsToAssign = newVarsToAssign;
		if(preassignment != null)
			varsToAssign.removeAll(preassignment.keySet());

		//Calculate var domain sizes
		Map<GdlVariable, Integer> varDomainSizes = getVarDomainSizes(varDomains/*rule, model*/);

		List<Integer> sourceConjunctSizes = new ArrayList<Integer>();
		for(GdlLiteral conjunct : rule.getBody()) {
			if(conjunct instanceof GdlRelation) {
				SentenceForm form = sentenceFormSource.getSentenceForm((GdlRelation)conjunct);
				if(completedSentenceFormSizes != null
						&& completedSentenceFormSizes.containsKey(form)) {
					int size = completedSentenceFormSizes.get(form);
					//New: Don't add if it will be useless as a source
					//For now, we take a strict definition of that
					//Compare its size with the product of the domains
					//of the variables it defines
					//In the future, we could require a certain ratio
					//to decide that this is worthwhile
					GdlRelation relation = (GdlRelation) conjunct;
					int maxSize = 1;
					Set<GdlVariable> vars = new HashSet<GdlVariable>(GdlUtils.getVariables(relation));
					for(GdlVariable var : vars) {
						int domainSize = varDomainSizes.get(var);
						maxSize *= domainSize;
					}
					if(size >= maxSize)
						continue;
					sourceConjunctCandidates.add(relation);
					sourceConjunctSizes.add(size);
				}
			}
		}

		List<GdlSentence> constantFormSentences = new ArrayList<GdlSentence>();
		List<ConstantForm> constantFormsAsList = new ArrayList<ConstantForm>();
		for(GdlLiteral conjunct : rule.getBody()) {
			if(conjunct instanceof GdlSentence) {
				SentenceForm form = sentenceFormSource.getSentenceForm((GdlSentence) conjunct);
				if(constantForms != null && constantForms.containsKey(form)) {
					constantFormSentences.add((GdlSentence) conjunct);
					constantFormsAsList.add(constantForms.get(form));
				}
			}
		}

		//TODO: If we have a head assignment, treat everything as already replaced
		//Maybe just translate the rule? Or should we keep the pool clean?

		IterationOrderCandidate emptyCandidate = new IterationOrderCandidate(varsToAssign, sourceConjunctCandidates,
				sourceConjunctSizes, constantFormSentences, constantFormsAsList, varDomainSizes);
		PriorityQueue<IterationOrderCandidate> searchQueue = new PriorityQueue<IterationOrderCandidate>();
		searchQueue.add(emptyCandidate);

		while(!searchQueue.isEmpty()) {
			IterationOrderCandidate curNode = searchQueue.remove();
			if(curNode.isComplete())
				//This is the complete ordering with the lowest heuristic value
				return curNode;
			searchQueue.addAll(curNode.getChildren(analyticFunctionOrdering));
		}
		throw new RuntimeException("Found no complete iteration orderings");
	}

	private static Map<GdlVariable, Integer> getVarDomainSizes(/*GdlRule rule,
			SentenceModel model*/Map<GdlVariable, Set<GdlConstant>> varDomains) {
		Map<GdlVariable, Integer> varDomainSizes = new HashMap<GdlVariable, Integer>();
		//Map<GdlVariable, Set<GdlConstant>> varDomains = model.getVarDomains(rule);
		for(GdlVariable var : varDomains.keySet()) {
			varDomainSizes.put(var, varDomains.get(var).size());
		}
		return varDomainSizes;
	}

	private static class IterationOrderCandidate implements Comparable<IterationOrderCandidate> {
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
		private List<GdlSentence> constantFormSentences;
		private List<ConstantForm> constantForms; //Indexing same as constantFormSentences
		private Map<GdlVariable, Integer> varDomainSizes;

		/**
		 * This constructor is for creating the start node of the
		 * search. No part of the ordering is specified.
		 *
		 * @param sourceConjunctCandidates
		 * @param sourceConjunctSizes
		 * @param constantFormSentences
		 * @param constantForms
		 * @param allVars
		 * @param varDomainSizes
		 */
		public IterationOrderCandidate(
				List<GdlVariable> varsToAssign,
				List<GdlSentence> sourceConjunctCandidates,
				List<Integer> sourceConjunctSizes,
				List<GdlSentence> constantFormSentences,
				List<ConstantForm> constantForms,
				Map<GdlVariable, Integer> varDomainSizes) {
			sourceConjunctIndices = new ArrayList<Integer>();
			varOrdering = new ArrayList<GdlVariable>();
			functionalConjunctIndices = new ArrayList<Integer>();
			varSources = new ArrayList<Integer>();

			this.varsToAssign = varsToAssign;
			this.sourceConjunctCandidates = sourceConjunctCandidates;
			this.sourceConjunctSizes = sourceConjunctSizes;
			this.constantFormSentences = constantFormSentences;
			this.constantForms = constantForms;
			this.varDomainSizes = varDomainSizes;
		}

		public List<GdlSentence> getFunctionalConjuncts() {
			//Returns, for each var, the conjunct defining it (if any)
			List<GdlSentence> functionalConjuncts = new ArrayList<GdlSentence>(functionalConjunctIndices.size());
			for(int index : functionalConjunctIndices) {
				if(index == -1)
					functionalConjuncts.add(null);
				else
					functionalConjuncts.add(constantFormSentences.get(index));
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
			this.constantFormSentences = parent.constantFormSentences;
			this.constantForms = parent.constantForms;
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
			this.constantFormSentences = parent.constantFormSentences;
			this.constantForms = parent.constantForms;
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
		 * @param constantForm
		 * @param bestVariable
		 */
		public IterationOrderCandidate(
				IterationOrderCandidate parent,
				GdlSentence constantFormSentence,
				int constantFormIndex, GdlVariable functionOutput) {
			//Shared rules:
			this.varsToAssign = parent.varsToAssign;
			this.sourceConjunctCandidates = parent.sourceConjunctCandidates;
			this.sourceConjunctSizes = parent.sourceConjunctSizes;
			this.constantFormSentences = parent.constantFormSentences;
			this.constantForms = parent.constantForms;
			this.varDomainSizes = parent.varDomainSizes;

			//Individual rules:
			sourceConjunctIndices = new ArrayList<Integer>(parent.sourceConjunctIndices);
			varOrdering = new ArrayList<GdlVariable>(parent.varOrdering);
			functionalConjunctIndices = new ArrayList<Integer>(parent.functionalConjunctIndices);
			varSources = new ArrayList<Integer>(parent.varSources);
			//And we add the function
			List<GdlVariable> varsInFunction = GdlUtils.getVariables(constantFormSentence);
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
			functionalConjunctIndices.add(constantFormIndex);
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

			return heuristic;
		}
		public boolean isComplete() {
			return varOrdering.containsAll(varsToAssign);
		}
		public List<IterationOrderCandidate> getChildren(boolean analyticFunctionOrdering) {
			List<IterationOrderCandidate> allChildren = new ArrayList<IterationOrderCandidate>();
			allChildren.addAll(getSourceConjunctChildren());
			allChildren.addAll(getFunctionAddedChildren(analyticFunctionOrdering));
			return allChildren;
		}
		private List<IterationOrderCandidate> getSourceConjunctChildren() {
			List<IterationOrderCandidate> children = new ArrayList<IterationOrderCandidate>();

			//If we are already using functions, short-circuit to cut off
			//repetition of the search space
			for(int index : functionalConjunctIndices)
				if(index != -1)
					return Collections.emptyList();

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
			if(analyticFunctionOrdering && constantForms.size() > 8) {
				//For each variable, a list of functions
				//(refer to functions by their indices)
				//and the set of outstanding vars they depend on...
				Map<GdlVariable, Set<Integer>> functionsProducingVars = new HashMap<GdlVariable, Set<Integer>>();
				//We start by adding to the varOrdering the vars not produced by functions
				//First, we have to find them
				for(int i = 0; i < constantForms.size(); i++) {
					GdlSentence cfs = constantFormSentences.get(i);
					ConstantForm cf = constantForms.get(i);
					Set<GdlVariable> producibleVars = cf.getProducibleVars(cfs);
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
				for(int i = 0; i < constantForms.size(); i++) {
					GdlSentence cfs = constantFormSentences.get(i);
					ConstantForm cf = constantForms.get(i);
					Set<GdlVariable> producibleVars = cf.getProducibleVars(cfs);
					Set<GdlVariable> allVars = new HashSet<GdlVariable>(GdlUtils.getVariables(cfs));
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
					for(int function : functions) {
						GdlSentence cfs = constantFormSentences.get(function);
						ConstantForm cf = constantForms.get(function);
						Set<GdlVariable> producibleVars = cf.getProducibleVars(cfs);
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
				if(!functionalConjunctIndices.isEmpty())
					lastFunctionUsedIndex = Collections.max(functionalConjunctIndices);
				Set<GdlVariable> varsProducedByFunctions = new HashSet<GdlVariable>();
				for(int i = 0; i < functionalConjunctIndices.size(); i++)
					if(functionalConjunctIndices.get(i) != -1)
						varsProducedByFunctions.add(varOrdering.get(i));
				for(int i = 0; i < constantForms.size(); i++) {
					GdlSentence constantFormSentence = constantFormSentences.get(i);
					ConstantForm constantForm = constantForms.get(i);

					if(i < lastFunctionUsedIndex) {
						//We need to figure out whether i could use any of the
						//vars we're producing with functions
						//TODO: Try this with a finer grain
						//i.e., see if i needs a var from a function that is after
						//it, not one that might be before it
						List<GdlVariable> varsInSentence = GdlUtils.getVariables(constantFormSentence);
						if(Collections.disjoint(varsInSentence, varsProducedByFunctions))
							continue;
					}

					//What is the best variable to grab from this form, if there are any?
					GdlVariable bestVariable = getBestVariable(constantFormSentence, constantForm);
					if(bestVariable == null)
						continue;
					IterationOrderCandidate newCandidate =
						new IterationOrderCandidate(this, constantFormSentence, i, bestVariable);
					children.add(newCandidate);
				}

				//If there are no more functions to add, add the completed version
				if(children.isEmpty()) {
					children.add(new IterationOrderCandidate(this));
				}
				return children;
			}
		}
		private GdlVariable getBestVariable(GdlSentence constantFormSentence,
				ConstantForm constantForm) {
			//If all the variables that can be set by the constant form are in
			//the varOrdering, we return null. Otherwise, we return one of
			//those with the largest domain.

			//The ConstantForm is sentence-independent, so we need the context
			//of the sentence (which has variables in it).
			List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(constantFormSentence);
			List<Boolean> dependentSlots = constantForm.getDependentSlots();
			if(tuple.size() != dependentSlots.size())
				throw new RuntimeException("Mismatched sentence " + constantFormSentence + " and constant form " + constantForm);

			Set<GdlVariable> candidateVars = new HashSet<GdlVariable>();
			for(int i = 0; i < tuple.size(); i++) {
				GdlTerm term = tuple.get(i);
				if(term instanceof GdlVariable && dependentSlots.get(i)
						&& !varOrdering.contains(term)
						&& varsToAssign.contains(term))
					candidateVars.add((GdlVariable) term);
			}
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


	public static long getNumAssignmentsEstimate(GdlRule rule, Map<GdlVariable, Set<GdlConstant>> varDomains, ConstantChecker checker, boolean analyticFunctionOrdering, SentenceFormSource sentenceFormSource) throws InterruptedException {
		//First we need the best iteration order
		//Arguments we'll need to pass in:
		//- A SentenceModel
		//- constant forms
		//- completed sentence form sizes
		//- Variable domain sizes?

		Map<SentenceForm, ConstantForm> constantForms = new HashMap<SentenceForm, ConstantForm>();
		for(SentenceForm form : /*model.getConstantSentenceForms()*/checker.getSentenceForms())
			constantForms.put(form, new ConstantForm(form, checker));

		//Populate variable domain sizes using the constant checker
		Map<SentenceForm, Integer> domainSizes = new HashMap<SentenceForm, Integer>();
		for(SentenceForm form : checker.getSentenceForms()) {
			domainSizes.put(form, checker.getNumTrueTuples(form));
		}
		//TODO: Propagate these domain sizes as estimates for other rules?
		//Look for literals in the body of the rule and their ancestors?
		//Could we possibly do this elsewhere?

		IterationOrderCandidate ordering = getBestIterationOrderCandidate(rule, /*model,*/varDomains, constantForms, null, null, analyticFunctionOrdering, sentenceFormSource);
		return ordering.getHeuristicValue();
	}
}