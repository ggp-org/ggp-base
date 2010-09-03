/**
 * 
 */
package util.propnet.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;
import util.gdl.model.SentenceModel;
import util.gdl.model.SentenceModel.SentenceForm;
import util.gdl.model.SentenceModel.TermModel;
import util.gdl.transforms.CommonTransforms;
import util.gdl.transforms.ConstantFinder.ConstantChecker;
import util.propnet.architecture.Component;
import util.propnet.architecture.components.Constant;

public class Assignments implements Iterable<Map<GdlVariable, GdlConstant>> {
	boolean empty;
	boolean allDone = false;
	//Contains all the assignments of variables we could make
	Map<GdlVariable, GdlConstant> headAssignment = new HashMap<GdlVariable, GdlConstant>();

	private List<GdlVariable> varsToAssign;
	private List<List<GdlConstant>> valuesToIterate;
	private List<AssignmentFunction> valuesToCompute;
	private List<Integer> indicesToChangeWhenNull;
	List<GdlDistinct> distincts;
	List<GdlVariable> varsToChangePerDistinct;

	public Assignments(Map<GdlVariable, GdlConstant> headAssignment,
			GdlRule rule, SentenceModel model, Map<SentenceForm, ConstantForm> constantForms) {
		empty = false;
		this.headAssignment = headAssignment;
		
		//We first have to find the remaining variables in the body
		varsToAssign = new ArrayList<GdlVariable>(SentenceModel.getVariables(rule));
		//Remove all the duplicates; we do, however, want to keep the ordering
		List<GdlVariable> newVarsToAssign = new ArrayList<GdlVariable>();
		for(GdlVariable v : varsToAssign)
			if(!newVarsToAssign.contains(v))
				newVarsToAssign.add(v);
		varsToAssign = newVarsToAssign;
		varsToAssign.removeAll(headAssignment.keySet());

		//Note that we're currently free to rearrange these as we
		//see fit. This may or may not help, and might be a topic
		//for further exploration.
		//For each variable, we want to see whether it could be
		//defined by a function somewhere in the rule, or whether
		//it would be better off as something to iterate over.

		//For each of these vars, we have to find one or the other.
		//Let's start by finding all the domains, a task already done.
		valuesToIterate = new ArrayList<List<GdlConstant>>(varsToAssign.size());

		for(GdlVariable var : varsToAssign) {
			valuesToIterate.add(new ArrayList<GdlConstant>());
		}
		for(GdlLiteral conjunct : rule.getBody()) {
			if(conjunct instanceof GdlRelation) {
				//This is where variables must be assigned
				for(int i = 0; i < varsToAssign.size(); i++) {
					GdlVariable var = varsToAssign.get(i);
					Set<GdlConstant> domain = getDomainInRelation((GdlRelation) conjunct, var, model);
					if(domain != null) {
						//Add to the domain
						if(valuesToIterate.get(i).isEmpty()) {
							valuesToIterate.get(i).addAll(domain);
						} else {
							valuesToIterate.get(i).retainAll(domain);
							if(valuesToIterate.get(i).isEmpty()) {
								//The game probably has an error in this rule
								System.out.println("Warning: Probable error in rule " + rule + ": check domains for variable " + var);
								valuesToIterate.get(i).addAll(domain);
							}
						}
					}
				}
			}
		}

		//We now want to see which we can give assignment functions to
		valuesToCompute = new ArrayList<AssignmentFunction>(varsToAssign.size());
		for(GdlVariable var : varsToAssign) {
			valuesToCompute.add(null);
		}
		indicesToChangeWhenNull = new ArrayList<Integer>(varsToAssign.size());
		for(int i = 0; i < varsToAssign.size(); i++) {
			//Change itself, why not?
			indicesToChangeWhenNull.add(i);
		}
		for(GdlLiteral conjunct : rule.getBody()) {
			if(conjunct instanceof GdlRelation) {
				//These are the only ones that could be constant functions
				SentenceForm conjForm = model.getSentenceForm((GdlSentence) conjunct);
				ConstantForm constForm = null;

				if(constantForms != null)
					constForm = constantForms.get(conjForm);
				if(constForm != null) {
					//Now we need to figure out which variables are involved
					//and which are suitable as functional outputs. 

					//1) Which vars are in this conjunct?
					List<GdlVariable> varsInSentence = SentenceModel.getVariables(conjunct);
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
					AssignmentFunction function = new AssignmentFunction((GdlRelation)conjunct, constForm, rightmostVar, varsToAssign);
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
	
	public Assignments() {
		//The assignment is impossible; return nothing
		empty = true;
	}
	public Assignments(GdlRule rule, SentenceModel model,
			Map<SentenceForm, ConstantForm> constantForms) {
		this(Collections.EMPTY_MAP, rule, model, constantForms);
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
	private Set<GdlConstant> getDomainInRelation(GdlRelation relation,
			GdlVariable var, SentenceModel model) {
		//Traverse the model and relation together
		Set<GdlConstant> domain = new HashSet<GdlConstant>();
		setDomainInRelation(domain, relation.getBody(), 
				model.getBody(relation.getName().getValue()), var);
		if(domain.isEmpty())
			return null;
		return domain;
	}
	private void setDomainInRelation(Set<GdlConstant> domain,
			List<GdlTerm> gdlBody, List<TermModel> modelBody, GdlVariable var) {
		for(int i = 0; i < gdlBody.size(); i++) {
			GdlTerm term = gdlBody.get(i);
			TermModel termModel = modelBody.get(i);
			if(term.equals(var)) {
				if(domain.isEmpty()) {
					domain.addAll(termModel.getConstants());
				} else {
					domain.retainAll(termModel.getConstants());
				}
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<TermModel> functionModelBody = termModel.getFunction(function);
				setDomainInRelation(domain, function.getBody(), functionModelBody, var);
			}
		}
	}
	@Override
	public Iterator<Map<GdlVariable, GdlConstant>> iterator() {
		return new AssignmentIterator();
	}
	public AssignmentIterator getIterator() {
		return new AssignmentIterator();
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

	//I suspect I'll be rewriting this whole class...
	//At least I'll be factoring out details about incrementing
	//a particular slot, for example
	public class AssignmentIterator implements Iterator<Map<GdlVariable, GdlConstant>> {
		//This time we just have integers to deal with
		List<Integer> valueIndices = new ArrayList<Integer>();
		List<GdlConstant> nextAssignment = new ArrayList<GdlConstant>();
		Map<GdlVariable, GdlConstant> assignmentMap = new HashMap<GdlVariable, GdlConstant>();

		boolean headOnly = false;
		boolean done = false;

		public AssignmentIterator() {
			if(varsToAssign == null) {
				headOnly = true;
				return;
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
				changeOneInNext(Collections.singletonList(varToChange));
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

		private void updateNextAssignment() {
			for(int i = 0; i < valueIndices.size(); i++) {
				if(valuesToCompute == null || valuesToCompute.get(i) == null) {
					nextAssignment.set(i, valuesToIterate.get(i).get(valueIndices.get(i)));
				} else {
					//Note that the values on the left must already be filled in
					nextAssignment.set(i, valuesToCompute.get(i).getValue(nextAssignment));
				}
			}
		}

		public void changeOneInNext(List<GdlVariable> vars) {
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
		public void changeOneInNext(List<GdlVariable> varsToChange,
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
	
	
	public static Assignments getAssignmentsWithRecursiveInput(GdlRule rule,
			SentenceModel model, SentenceForm form, GdlSentence input,
			Map<SentenceForm, ConstantForm> constantForms, boolean useConstForms) {
		//Look for the literal(s) in the rule with the sentence form of the
		//recursive input. This can be tricky if there are multiple matching
		//literals.
		List<GdlSentence> matchingLiterals = new ArrayList<GdlSentence>();
		for(GdlLiteral literal : rule.getBody())
			if(literal instanceof GdlSentence)
				if(form.matches((GdlSentence) literal))
					matchingLiterals.add((GdlSentence) literal);

		List<Assignments> assignmentsList = new ArrayList<Assignments>();
		for(GdlSentence matchingLiteral : matchingLiterals) {
			//left has the variables, right has the constants
			Map<GdlVariable, GdlConstant> preassignment = getAssignmentMakingLeftIntoRight(matchingLiteral, input);
			if(preassignment != null) {
				Assignments assignments = new Assignments(preassignment, rule, model, constantForms);
				assignmentsList.add(assignments);
			}
		}

		if(assignmentsList.size() == 0)
			return new Assignments();
		if(assignmentsList.size() == 1)
			return assignmentsList.get(0);
		throw new RuntimeException("Not yet implemented: assignments for recursive functions with multiple recursive conjuncts");
		//TODO: Plan to implement by subclassing Assignments into something
		//that contains and iterates over multiple Assignments
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

	public static Assignments getAssignmentsProducingSentence(
			GdlRule rule, GdlSentence sentence, SentenceModel model,
			Map<SentenceForm, ConstantForm> constantForms) {
		//First, we see which variables must be set according to the rule head
		//(and see if there's any contradiction)
		Map<GdlVariable, GdlConstant> headAssignment = new HashMap<GdlVariable, GdlConstant>();
		if(!setVariablesInHead(rule.getHead(), sentence, headAssignment)) {
			return new Assignments();//Collections.emptySet();
		}
		//Then we come up with all the assignments of the rest of the variables
		if(headAssignment == null)
			System.out.println("headAssignment is null over here, too");

		//We need to look for functions we can make use of

		return new Assignments(headAssignment, rule, model, constantForms);
	}

	public static Assignments getAssignmentsForRule(GdlRule rule,
			SentenceModel model, Map<SentenceForm, ConstantForm> constantForms) {
		return new Assignments(rule, model, constantForms);
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
		SentenceForm form;

		int numSlots;
		//True iff the slot has at most one value given the other slots' values
		List<Boolean> dependentSlots = new ArrayList<Boolean>();
		List<Map<List<GdlConstant>, GdlConstant>> valueMaps = new ArrayList<Map<List<GdlConstant>, GdlConstant>>();;

		public ConstantForm(SentenceForm form, Map<GdlSentence, Component> values) {
			this.form = form;

			numSlots = form.getTupleSize();

			List<List<GdlConstant>> tuples = getTrueTuples(form, values);

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
		
		public ConstantForm(SentenceForm form, ConstantChecker constantChecker) {
			this.form = form;

			numSlots = form.getTupleSize();

			for(int i = 0; i < numSlots; i++) {
				//We want to establish whether or not this is a constant...
				Map<List<GdlConstant>, GdlConstant> functionMap = new HashMap<List<GdlConstant>, GdlConstant>();
				boolean functional = true;
				Iterator<List<GdlConstant>> itr = constantChecker.getTrueTuples(form);
				while(itr.hasNext()) {
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
				Map<GdlSentence, Component> values) {
			List<List<GdlConstant>> tuples = new ArrayList<List<GdlConstant>>();
			//Working on the assumption that this will be faster than
			//going through all the keys in "values". I suppose I could
			//actually CHECK that this is the case, if it becomes an issue.
			for(GdlSentence sentence : form) {
				if(values.containsKey(sentence)) {
					Component value = values.get(sentence);
					if(value instanceof Constant && value.getValue()) {
						//It's a true constant
						//Add the tuple
						tuples.add(SentenceModel.getTupleFromGroundSentence(sentence));
					}
				}
			}
			return tuples;
		}

		public Map<List<GdlConstant>, GdlConstant> getValueMap(int index) {
			return valueMaps.get(index);
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
				GdlVariable rightmostVar, List<GdlVariable> varOrder) {
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


}