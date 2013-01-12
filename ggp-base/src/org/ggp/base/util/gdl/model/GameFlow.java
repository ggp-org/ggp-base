package org.ggp.base.util.gdl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.transforms.CommonTransforms;
import org.ggp.base.util.gdl.transforms.ConstantFinder;
import org.ggp.base.util.gdl.transforms.DeORer;
import org.ggp.base.util.gdl.transforms.GdlCleaner;
import org.ggp.base.util.gdl.transforms.VariableConstrainer;
import org.ggp.base.util.gdl.transforms.ConstantFinder.ConstantChecker;
import org.ggp.base.util.propnet.factory.AssignmentIterator;
import org.ggp.base.util.propnet.factory.Assignments;
import org.ggp.base.util.propnet.factory.AssignmentsFactory;
import org.ggp.base.util.propnet.factory.AssignmentsImpl.ConstantForm;


/**
 * GameFlow describes the behavior of the sentences in sentence forms that depend
 * on which turn it is, but not on the actions of the player (past or present).
 * These include step counters and control markers.
 *  
 * @author Alex Landau
 */
public class GameFlow {
	private static final GdlConstant INIT = GdlPool.getConstant("init");
	private static final GdlConstant TRUE = GdlPool.getConstant("true");
	private static final GdlConstant NEXT = GdlPool.getConstant("next");
	
	int turnAfterLast; //We end with a loop
	List<Set<GdlSentence>> sentencesTrueByTurn = new ArrayList<Set<GdlSentence>>(); //The non-constant ones
	Set<SentenceForm> formsControlledByFlow;
	Set<SentenceForm> constantForms;
	ConstantChecker constantChecker;

	public GameFlow(List<Gdl> description) throws InterruptedException {
		
		description = GdlCleaner.run(description);
		description = DeORer.run(description);
		description = VariableConstrainer.replaceFunctionValuedVariables(description);
		
		//First we use a sentence model to get the relevant sentence forms
		SentenceModel model = new SentenceModelImpl(description);
		formsControlledByFlow = new HashSet<SentenceForm>();
		formsControlledByFlow.addAll(model.getIndependentSentenceForms());
		formsControlledByFlow.removeAll(model.getConstantSentenceForms());
		constantForms = model.getConstantSentenceForms();
		
		//System.out.println("Setting constants for the game flow...");
		constantChecker = ConstantFinder.getConstants(description);
		//System.out.println("Done setting constants");
		
		//Figure out which of these sentences are true at each stage
		solveTurns(model);
	}

	private void solveTurns(SentenceModel model) throws InterruptedException {
		//Before we can do anything else, we need a topological ordering on our forms
		List<SentenceForm> ordering = getTopologicalOrdering(model.getIndependentSentenceForms(), model.getDependencyGraph());
		ordering.retainAll(formsControlledByFlow);
		
		//Let's add constant forms ("constForms") to the consideration...
		Map<SentenceForm, ConstantForm> constForms = new HashMap<SentenceForm, ConstantForm>();
		for(SentenceForm form : constantForms) {
			constForms.put(form, new ConstantForm(form, constantChecker));
		}
		
		//First we set the "true" values, then we get the forms controlled by the flow...
		//Use "init" values
		Set<GdlSentence> trueFlowSentences = new HashSet<GdlSentence>();
		for(SentenceForm form : constantForms) {
			if(form.getName().equals(INIT)) {
				Iterator<GdlSentence> itr = constantChecker.getTrueSentences(form);
				while(itr.hasNext()) {
					GdlSentence initSentence = itr.next();
					GdlSentence trueSentence = GdlPool.getRelation(TRUE, initSentence.getBody());
					trueFlowSentences.add(trueSentence);
				}
			}
		}
		//Go through ordering, adding to trueFlowSentences
		addSentenceForms(ordering, trueFlowSentences, model, constForms);
		sentencesTrueByTurn.add(trueFlowSentences);
		
		outer : while(true) {
			//Now we use the "next" values from the previous turn
			Set<GdlSentence> sentencesPreviouslyTrue = trueFlowSentences;
			trueFlowSentences = new HashSet<GdlSentence>();
			for(GdlSentence sentence : sentencesPreviouslyTrue) {
				if(sentence.getName().equals(NEXT)) {
					GdlSentence trueSentence = GdlPool.getRelation(TRUE, sentence.getBody());
					trueFlowSentences.add(trueSentence);
				}
			}
			
			addSentenceForms(ordering, trueFlowSentences, model, constForms);
			
			//Test if this turn's flow is the same as an earlier one
			for(int i = 0; i < sentencesTrueByTurn.size(); i++) {
				Set<GdlSentence> prevSet = sentencesTrueByTurn.get(i);
				if(prevSet.equals(trueFlowSentences)) {
					//Complete the loop
					turnAfterLast = i;
					break outer;
				}
			}
			sentencesTrueByTurn.add(trueFlowSentences);
		}
		
		//System.out.println("Found a " + getNumTurns() + "-turn flow");
	}
	
	@SuppressWarnings("unchecked")
	private void addSentenceForms(List<SentenceForm> ordering,
			Set<GdlSentence> trueFlowSentences, SentenceModel model,
			Map<SentenceForm, ConstantForm> constForms) {
		for(SentenceForm curForm : ordering) {
			//Check against trueFlowSentences, add to trueFlowSentences
			//or check against constantForms if necessary
			//Use basic Assignments class, of course
			
			for(GdlRelation relation : model.getRelations(curForm))
				trueFlowSentences.add(relation);
			for(GdlRule rule : model.getRules(curForm)) {
				GdlSentence head = rule.getHead();
				List<GdlVariable> varsInHead = GdlUtils.getVariables(head);
				Assignments assignments = AssignmentsFactory.getAssignmentsForRule(rule, model, constForms, Collections.EMPTY_MAP);

				AssignmentIterator asnItr = assignments.getIterator();
				while(asnItr.hasNext()) {
					Map<GdlVariable, GdlConstant> assignment = asnItr.next();
					boolean isGoodAssignment = true;
					
					GdlSentence transformedHead = CommonTransforms.replaceVariables(head, assignment);
					if(trueFlowSentences.contains(transformedHead))
						asnItr.changeOneInNext(varsInHead, assignment);
					
					//Go through the conjuncts
					for(GdlLiteral literal : rule.getBody()) {
						if(literal instanceof GdlSentence) {
							if(curForm.matches((GdlSentence) literal))
								throw new RuntimeException("Haven't implemented recursion in the game flow");
							GdlSentence transformed = CommonTransforms.replaceVariables((GdlSentence) literal, assignment);
							SentenceForm conjForm = model.getSentenceForm(transformed);
							if(constantForms.contains(conjForm)) {
								if(!constantChecker.isTrueConstant(transformed)) {
									isGoodAssignment = false;
									asnItr.changeOneInNext(GdlUtils.getVariables(literal), assignment);
								}
							} else {
								if(!trueFlowSentences.contains(transformed)) {
									//False sentence
									isGoodAssignment = false;
									asnItr.changeOneInNext(GdlUtils.getVariables(literal), assignment);
								}
							}
						} else if(literal instanceof GdlNot) {
							GdlSentence internal = (GdlSentence) ((GdlNot) literal).getBody();
							GdlSentence transformed = CommonTransforms.replaceVariables(internal, assignment);
							SentenceForm conjForm = model.getSentenceForm(transformed);
							
							if(constantForms.contains(conjForm)) {
								if(constantChecker.isTrueConstant(transformed)) {
									isGoodAssignment = false;
									asnItr.changeOneInNext(GdlUtils.getVariables(literal), assignment);
								}
							} else {
								if(trueFlowSentences.contains(transformed)) {
									//False sentence
									isGoodAssignment = false;
									asnItr.changeOneInNext(GdlUtils.getVariables(literal), assignment);
								}
							}
							
						}
						//Nothing else needs attention, really
					}
					
					//We've gone through all the conjuncts and are at the
					//end of the rule
					if(isGoodAssignment) {
						trueFlowSentences.add(transformedHead);
						if(varsInHead.isEmpty())
							break; //out of the assignments for this rule
						else
							asnItr.changeOneInNext(varsInHead, assignment);
					}
				}
			}
			//We've gone through all the rules
		}
	}

	public int getNumTurns() {
		return sentencesTrueByTurn.size();
	}

	private static List<SentenceForm> getTopologicalOrdering(
			Set<SentenceForm> forms,
			Map<SentenceForm, Set<SentenceForm>> dependencyGraph) {
		//We want each form as a key of the dependency graph to
		//follow all the forms in the dependency graph, except maybe itself
		Queue<SentenceForm> queue = new LinkedList<SentenceForm>(forms);
		List<SentenceForm> ordering = new ArrayList<SentenceForm>(forms.size());
		Set<SentenceForm> alreadyOrdered = new HashSet<SentenceForm>();
		while(!queue.isEmpty()) {
			SentenceForm curForm = queue.remove();
			boolean readyToAdd = true;
			//Don't add if there are dependencies
			if(dependencyGraph.get(curForm) != null) {
				for(SentenceForm dependency : dependencyGraph.get(curForm)) {
					if(!dependency.equals(curForm) && !alreadyOrdered.contains(dependency)) {
						readyToAdd = false;
						break;
					}
				}
			}
			//Add it
			if(readyToAdd) {
				ordering.add(curForm);
				alreadyOrdered.add(curForm);
			} else {
				queue.add(curForm);
			}
			//TODO: Add check for an infinite loop here
			//Or replace with code that does stratification of loops
		}
		return ordering;
	}


	public Set<Integer> getTurnsConjunctsArePossible(List<GdlLiteral> body) {
		//We want to identify the conjuncts that are used by the
		//game flow.
		List<GdlLiteral> relevantLiterals = new ArrayList<GdlLiteral>();
		for(GdlLiteral literal : body) {
			if(literal instanceof GdlSentence) {
				GdlSentence sentence = (GdlSentence) literal;
				if(SentenceModelUtils.inSentenceFormGroup(sentence, formsControlledByFlow))
					relevantLiterals.add(literal);
			} else if(literal instanceof GdlNot) {
				GdlNot not = (GdlNot) literal;
				GdlSentence innerSentence = (GdlSentence) not.getBody();
				if(SentenceModelUtils.inSentenceFormGroup(innerSentence, formsControlledByFlow))
					relevantLiterals.add(literal);
			}
		}
		
		//If none are related to the game flow, then that's it. It can
		//happen on any turn.
		//if(relevantLiterals.isEmpty())
			//return getCompleteTurnSet();
		Set<Integer> turnsPossible = new HashSet<Integer>(getCompleteTurnSet());
		
		//For each of the relevant literals, we need to see if there are assignments
		//such that 
		for(GdlLiteral literal : relevantLiterals) {
			List<Integer> turns = new ArrayList<Integer>();
			if(literal instanceof GdlSentence) {
				for(int t = 0; t < getNumTurns(); t++) {
					if(sentencesTrueByTurn.get(t).contains(literal))
						turns.add(t);
					else for(GdlSentence s : sentencesTrueByTurn.get(t)) {
						//Could be true if there's an assignment
						if(null != GdlUtils.getAssignmentMakingLeftIntoRight((GdlSentence)literal, s)) {
							turns.add(t);
							break;
						}
					}
				}
			} else if(literal instanceof GdlNot) {
				GdlNot not = (GdlNot) literal;
				GdlSentence internal = (GdlSentence) not.getBody();
				for(int t = 0; t < getNumTurns(); t++) {
					if(!sentencesTrueByTurn.get(t).contains(internal))
						turns.add(t);
					else for(GdlSentence s : sentencesTrueByTurn.get(t)) {
						if(null != GdlUtils.getAssignmentMakingLeftIntoRight(internal, s)) {
							turns.add(t);
							break;
						}
					}
				}
			}
			//Accumulate turns
			//Note that all relevant conjuncts must be true, so this
			//is an intersection of when the individual conjuncts
			//could be true.
			turnsPossible.retainAll(turns);
		}
		return turnsPossible;
	}

	private Set<Integer> completeTurnSet = null;
	public Set<Integer> getCompleteTurnSet() {
		if(completeTurnSet == null) {
			completeTurnSet = new HashSet<Integer>();
			for(int i = 0; i < getNumTurns(); i++) {
				completeTurnSet.add(i);
			}
			completeTurnSet = Collections.unmodifiableSet(completeTurnSet);
		}
		return completeTurnSet;
	}

	public Set<SentenceForm> getSentenceForms() {
		return formsControlledByFlow;
	}

	public Set<GdlSentence> getSentencesTrueOnTurn(int i) {
		return sentencesTrueByTurn.get(i);
	}

	public int getTurnAfterLast() {
		return turnAfterLast;
	}

}
