package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.collect.ImmutableList;

// Not thread-safe
public class AssignmentIteratorImpl implements AssignmentIterator {
		private List<Integer> sourceTupleIndices = null;
		//This time we just have integers to deal with
		private List<Integer> valueIndices = null;
		private List<GdlConstant> nextAssignment = new ArrayList<GdlConstant>();
		private Map<GdlVariable, GdlConstant> assignmentMap = new HashMap<GdlVariable, GdlConstant>();

		private boolean headOnly = false;
		private boolean done = false;
		private final AssignmentIterationPlan plan;

		public AssignmentIteratorImpl(AssignmentIterationPlan plan) {
			this.plan = plan;
			//TODO: Handle this case with a separate class
			if(plan.getVarsToAssign() == null) {
				headOnly = true;
				return;
			}

			//Set up source tuple...
			sourceTupleIndices = new ArrayList<Integer>(plan.getTuplesBySource().size());
			for(int i = 0; i < plan.getTuplesBySource().size(); i++) {
				sourceTupleIndices.add(0);
			}
			//Set up...
			valueIndices = new ArrayList<Integer>(plan.getVarsToAssign().size());
			for(int i = 0; i < plan.getVarsToAssign().size(); i++) {
				valueIndices.add(0);
				nextAssignment.add(null);
			}

			assignmentMap.putAll(plan.getHeadAssignment());

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
					incrementIndex(plan.getIndicesToChangeWhenNull().get(i));
					if(nextAssignment == null)
						return;
					i = -1;
				}
			}

			//Find all the unsatisfied distincts
			//Find the pair with the earliest var. that needs to be changed
			List<GdlVariable> varsToChange = new ArrayList<GdlVariable>();
			for(int d = 0; d < plan.getDistincts().size(); d++) {
				GdlDistinct distinct = plan.getDistincts().get(d);
				//The assignments must use the assignments implied by nextAssignment
				GdlConstant term1 = replaceVariables(distinct.getArg1());
				GdlConstant term2 = replaceVariables(distinct.getArg2());
				if(term1.equals(term2)) {
					//need to change one of these
					varsToChange.add(plan.getVarsToChangePerDistinct().get(d));
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
			for(GdlVariable var : plan.getVarsToAssign())
				if(vars.contains(var))
					return var;
			return null;
		}

		private GdlConstant replaceVariables(GdlTerm term) {
			if(term instanceof GdlFunction)
				throw new RuntimeException("Function in the distinct... not handled");
			//Use the assignments implied by nextAssignment
			if(plan.getHeadAssignment().containsKey(term))
				return plan.getHeadAssignment().get(term); //Translated in head assignment
			if(term instanceof GdlConstant)
				return (GdlConstant) term;
			int index = plan.getVarsToAssign().indexOf(term);
			return nextAssignment.get(index);
		}

		private void incrementIndex(int index) {
			if(index < 0) {
				//Trash the iterator
				nextAssignment = null;
				return;
			}
			if(plan.getValuesToCompute() != null && plan.getValuesToCompute().containsKey(index)) {
				//The constant at this index is functionally computed
				incrementIndex(index - 1);
				return;
			}
			if(plan.getSourceDefiningSlot().get(index) != -1) {
				//This is set by a source; increment the source
				incrementSource(plan.getSourceDefiningSlot().get(index));
				return;
			}
			//We try increasing the var at index by 1.
			//Everything to the right of it gets reset.
			//If it can't be increased, increase the number
			//to the left instead. If nothing can be
			//increased, trash the iterator.
			int curValue = valueIndices.get(index);
			if(curValue == plan.getValuesToIterate().get(index).size() - 1) {
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
			if(curValue == plan.getTuplesBySource().get(source).size() - 1) {
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
				ImmutableList<ImmutableList<GdlConstant>> tuples = plan.getTuplesBySource().get(s);
				int curIndex = sourceTupleIndices.get(s);
				if(tuples.size() == 0) {
					// This could happen if e.g. there are no tuples that agree with
					// the headAssignment.
					nextAssignment = null;
					return;
				}
				List<GdlConstant> tuple = tuples.get(curIndex);
				List<Integer> varsChosen = plan.getVarsChosenBySource().get(s);
				List<Boolean> putDontCheckTuple = plan.getPutDontCheckBySource().get(s);
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
				if((plan.getValuesToCompute() == null || !plan.getValuesToCompute().containsKey(i))
						&& plan.getSourceDefiningSlot().get(i) == -1) {
					nextAssignment.set(i, plan.getValuesToIterate().get(i).get(valueIndices.get(i)));
				} else if(plan.getSourceDefiningSlot().get(i) == -1) {
					//Fill in based on a function
					//Note that the values on the left must already be filled in
					GdlConstant valueFromFunction = plan.getValuesToCompute().get(i).getValue(nextAssignment);
//					System.out.println("Setting based on a function: slot " + i + " to value " + valueFromFunction);
					nextAssignment.set(i, valueFromFunction);
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
			if(plan.getVarsToAssign() == null)
				System.out.println("headOnly: " + headOnly);

			GdlVariable rightmostVar = getRightmostVar(vars);
			incrementIndex(plan.getVarsToAssign().indexOf(rightmostVar));
			makeNextAssignmentValid();

		}

		@Override
		public void changeOneInNext(Collection<GdlVariable> varsToChange,
				Map<GdlVariable, GdlConstant> assignment) {
			if (nextAssignment == null) {
				return;
			}

			//First, we stop and see if any of these have already been
			//changed (in nextAssignment)
			for (GdlVariable varToChange : varsToChange) {
				int index = plan.getVarsToAssign().indexOf(varToChange);
				if (index != -1) {
					GdlConstant assignedValue = assignment.get(varToChange);
					if (assignedValue == null) {
						throw new IllegalArgumentException("assignedValue is null; " +
								"varToChange is " + varToChange +
								" and assignment is " + assignment);
					}
					if (nextAssignment == null) {
						throw new IllegalStateException("nextAssignment is null");
					}
					if (!assignedValue.equals(nextAssignment.get(index))) {
						//We've already changed one of these
						return;
					}
				}
			}

			//Okay, we actually need to change one of these
			changeOneInNext(varsToChange);
		}


		@Override
		public boolean hasNext() {
			if (plan.getEmpty()) {
				return false;
			}
			if (headOnly) {
				return (!plan.getAllDone() && !done);
			}

			return (nextAssignment != null);
		}

		@Override
		public Map<GdlVariable, GdlConstant> next() {
			if(headOnly) {
				if(plan.getAllDone() || done)
					throw new RuntimeException("Asking for next when all done");
				done = true;
				return plan.getHeadAssignment();
			}

			updateMap(); //Sets assignmentMap

			//Adds one to the nextAssignment
			incrementIndex(valueIndices.size() - 1);
			makeNextAssignmentValid();

			return assignmentMap;
		}

		private void updateMap() {
			//Sets the map to match the nextAssignment
			for(int i = 0; i < plan.getVarsToAssign().size(); i++) {
				assignmentMap.put(plan.getVarsToAssign().get(i), nextAssignment.get(i));
			}
		}

		private GdlVariable getRightmostVar(Collection<GdlVariable> vars) {
			GdlVariable rightmostVar = null;
			for (GdlVariable var : plan.getVarsToAssign()) {
				if(vars.contains(var)) {
					rightmostVar = var;
				}
			}
			return rightmostVar;
		}

		@Override
		public void remove() {
			//Not implemented
		}
	}