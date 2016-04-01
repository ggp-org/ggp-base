/**
 *
 */
package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

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
import org.ggp.base.util.gdl.model.SimpleSentenceForm;
import org.ggp.base.util.gdl.transforms.CommonTransforms;
import org.ggp.base.util.gdl.transforms.ConstantChecker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


public class AssignmentsImpl implements Assignments {
    private boolean empty;
    private boolean allDone = false;
    //Contains all the assignments of variables we could make
    private Map<GdlVariable, GdlConstant> headAssignment;

    private List<GdlVariable> varsToAssign;
    private List<ImmutableList<GdlConstant>> valuesToIterate;
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

    private List<ImmutableList<ImmutableList<GdlConstant>>> tuplesBySource; //indexed by conjunct
    private List<Integer> sourceDefiningSlot; //indexed by var slot
    private List<ImmutableList<Integer>> varsChosenBySource; //indexed by conjunct, then slot
    private List<ImmutableList<Boolean>> putDontCheckBySource; //indexed by conjunct, then slot

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
     * @param functionInfoMap
     * @param completedSentenceFormValues
     */
    public AssignmentsImpl(Map<GdlVariable, GdlConstant> headAssignment,
            GdlRule rule, Map<GdlVariable, Set<GdlConstant>> varDomains,
            Map<SentenceForm, ? extends FunctionInfo> functionInfoMap,
            Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
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
        bestOrdering = getBestIterationOrderCandidate(rule, varDomains,/*model,*/ functionInfoMap, completedSentenceFormSizes, headAssignment, false); //TODO: True here?

        //Want to replace next few things with order
        //Need a few extra things to handle the use of iteration over existing tuples
        varsToAssign = bestOrdering.getVariableOrdering();

        //For each of these vars, we have to find one or the other.
        //Let's start by finding all the domains, a task already done.
        valuesToIterate = Lists.newArrayListWithCapacity(varsToAssign.size());

        for(GdlVariable var : varsToAssign) {
            if(varDomains.containsKey(var)) {
                if(!varDomains.get(var).isEmpty())
                    valuesToIterate.add(ImmutableList.copyOf(varDomains.get(var)));
                else
                    valuesToIterate.add(ImmutableList.of(GdlPool.getConstant("0")));
            } else {
                valuesToIterate.add(ImmutableList.of(GdlPool.getConstant("0")));
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
        tuplesBySource = Lists.newArrayListWithCapacity(sourceConjuncts.size());//new ArrayList<List<List<GdlConstant>>>(sourceConjuncts.size());
        varsChosenBySource = Lists.newArrayListWithCapacity(sourceConjuncts.size());//new ArrayList<List<Integer>>(sourceConjuncts.size());
        putDontCheckBySource = Lists.newArrayListWithCapacity(sourceConjuncts.size());//new ArrayList<List<Boolean>>(sourceConjuncts.size());
        for(int j = 0; j < sourceConjuncts.size(); j++) {
            GdlSentence sourceConjunct = sourceConjuncts.get(j);
            SentenceForm form = SimpleSentenceForm.create(sourceConjunct);
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
            varsChosenBySource.add(ImmutableList.copyOf(varsChosen));
            putDontCheckBySource.add(ImmutableList.copyOf(putDontCheck));

            //Now we put the tuples together
            //We use constraintSlots and constraintValues to check that the
            //tuples have compatible values
            Collection<GdlSentence> sentences = completedSentenceFormValues.get(form);
            List<ImmutableList<GdlConstant>> tuples = Lists.newArrayList();
            byTuple: for(GdlSentence sentence : sentences) {
                //Check that it doesn't conflict with our headAssignment
                if (!headAssignment.isEmpty()) {
                    Map<GdlVariable, GdlConstant> tupleAssignment = GdlUtils.getAssignmentMakingLeftIntoRight(sourceConjunct, sentence);
                    for (GdlVariable var : headAssignment.keySet()) {
                        if (tupleAssignment.containsKey(var)
                                && tupleAssignment.get(var) != headAssignment.get(var)) {
                            continue byTuple;
                        }
                    }
                }
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
                tuples.add(ImmutableList.copyOf(shortTuple));
            }
            //sortTuples(tuples); //Needed? Useful? Not sure. Probably not?
            tuplesBySource.add(ImmutableList.copyOf(tuples));
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
//      System.out.println("functionalConjuncts: " + functionalConjuncts);
        for(int i = 0; i < functionalConjuncts.size(); i++) {
            GdlSentence functionalConjunct = functionalConjuncts.get(i);
            if(functionalConjunct != null) {
                //These are the only ones that could be constant functions
                SentenceForm conjForm = SimpleSentenceForm.create(functionalConjunct);
                FunctionInfo functionInfo = null;

                if(functionInfoMap != null)
                    functionInfo = functionInfoMap.get(conjForm);
                if(functionInfo != null) {
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
                    AssignmentFunction function = AssignmentFunction.create((GdlRelation)functionalConjunct, functionInfo, rightmostVar, varsToAssign, headAssignment);
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
//      System.out.println("headAssignment: " + headAssignment);
//      System.out.println("varsToAssign: " + varsToAssign);
//      System.out.println("valuesToCompute: " + valuesToCompute);
//      System.out.println("sourceDefiningSlot: " + sourceDefiningSlot);
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
            Map<SentenceForm, ? extends FunctionInfo> functionInfoMap,
            Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
        this(Collections.EMPTY_MAP, rule, varDomains, functionInfoMap, completedSentenceFormValues);
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
        return new AssignmentIteratorImpl(getPlan());
    }
    @Override
    public AssignmentIterator getIterator() {
        return new AssignmentIteratorImpl(getPlan());
    }

    private AssignmentIterationPlan getPlan() {
        return AssignmentIterationPlan.create(varsToAssign,
                tuplesBySource,
                headAssignment,
                indicesToChangeWhenNull,
                distincts,
                varsToChangePerDistinct,
                valuesToCompute,
                sourceDefiningSlot,
                valuesToIterate,
                varsChosenBySource,
                putDontCheckBySource,
                empty,
                allDone);
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

    public static Assignments getAssignmentsProducingSentence(
            GdlRule rule, GdlSentence sentence, /*SentenceModel model,*/ Map<GdlVariable, Set<GdlConstant>> varDomains,
            Map<SentenceForm, FunctionInfo> functionInfoMap,
            Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
        //First, we see which variables must be set according to the rule head
        //(and see if there's any contradiction)
        Map<GdlVariable, GdlConstant> headAssignment = new HashMap<GdlVariable, GdlConstant>();
        if(!setVariablesInHead(rule.getHead(), sentence, headAssignment)) {
            return new AssignmentsImpl();//Collections.emptySet();
        }
        //Then we come up with all the assignments of the rest of the variables

        //We need to look for functions we can make use of

        return new AssignmentsImpl(headAssignment, rule, varDomains, functionInfoMap, completedSentenceFormValues);
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


    /**
     * Finds the iteration order (including variables, functions, and
     * source conjuncts) that is expected to result in the fastest iteration.
     *
     * The value that is compared for each ordering is the product of:
     * - For each source conjunct, the number of tuples offered by the conjunct;
     * - For each variable not defined by a function, the size of its domain.
     *
     * @param functionInfoMap
     * @param completedSentenceFormSizes For each sentence form, this may optionally
     * contain the number of possible sentences of this form. This is useful if the
     * number of sentences is much lower than the product of its variables' domain
     * sizes; however, if this contains sentence forms where the set of sentences
     * is unknown, then it may return an ordering that is unusable.
     */
    protected static IterationOrderCandidate getBestIterationOrderCandidate(GdlRule rule,
            /*SentenceModel model,*/
            Map<GdlVariable, Set<GdlConstant>> varDomains,
            Map<SentenceForm, ? extends FunctionInfo> functionInfoMap,
            Map<SentenceForm, Integer> completedSentenceFormSizes,
            Map<GdlVariable, GdlConstant> preassignment,
            boolean analyticFunctionOrdering) {
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
                SentenceForm form = SimpleSentenceForm.create((GdlRelation)conjunct);
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

        List<GdlSentence> functionalSentences = new ArrayList<GdlSentence>();
        List<FunctionInfo> functionalSentencesInfo = new ArrayList<FunctionInfo>();
        for(GdlLiteral conjunct : rule.getBody()) {
            if(conjunct instanceof GdlSentence) {
                SentenceForm form = SimpleSentenceForm.create((GdlSentence) conjunct);
                if(functionInfoMap != null && functionInfoMap.containsKey(form)) {
                    functionalSentences.add((GdlSentence) conjunct);
                    functionalSentencesInfo.add(functionInfoMap.get(form));
                }
            }
        }

        //TODO: If we have a head assignment, treat everything as already replaced
        //Maybe just translate the rule? Or should we keep the pool clean?

        IterationOrderCandidate emptyCandidate = new IterationOrderCandidate(varsToAssign, sourceConjunctCandidates,
                sourceConjunctSizes, functionalSentences, functionalSentencesInfo, varDomainSizes);
        PriorityQueue<IterationOrderCandidate> searchQueue = new PriorityQueue<IterationOrderCandidate>();
        searchQueue.add(emptyCandidate);

        while(!searchQueue.isEmpty()) {
            IterationOrderCandidate curNode = searchQueue.remove();
//          System.out.println("Node being checked out: " + curNode);
            if(curNode.isComplete()) {
                //This is the complete ordering with the lowest heuristic value
                return curNode;
            }
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

    public static long getNumAssignmentsEstimate(GdlRule rule, Map<GdlVariable, Set<GdlConstant>> varDomains, ConstantChecker checker) throws InterruptedException {
        //First we need the best iteration order
        //Arguments we'll need to pass in:
        //- A SentenceModel
        //- constant forms
        //- completed sentence form sizes
        //- Variable domain sizes?

        Map<SentenceForm, FunctionInfo> functionInfoMap = new HashMap<SentenceForm, FunctionInfo>();
        for (SentenceForm form : checker.getConstantSentenceForms()) {
            functionInfoMap.put(form, FunctionInfoImpl.create(form, checker));
        }

        //Populate variable domain sizes using the constant checker
        Map<SentenceForm, Integer> domainSizes = new HashMap<SentenceForm, Integer>();
        for(SentenceForm form : checker.getConstantSentenceForms()) {
            domainSizes.put(form, checker.getTrueSentences(form).size());
        }
        //TODO: Propagate these domain sizes as estimates for other rules?
        //Look for literals in the body of the rule and their ancestors?
        //Could we possibly do this elsewhere?

        IterationOrderCandidate ordering = getBestIterationOrderCandidate(rule, /*model,*/varDomains, functionInfoMap, null, null, true);
        return ordering.getHeuristicValue();
    }
}
