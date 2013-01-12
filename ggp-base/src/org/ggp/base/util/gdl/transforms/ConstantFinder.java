package org.ggp.base.util.gdl.transforms;

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

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceModel;
import org.ggp.base.util.gdl.model.SentenceModelImpl;
import org.ggp.base.util.gdl.model.SentenceModelUtils;
import org.ggp.base.util.propnet.factory.AssignmentIterator;
import org.ggp.base.util.propnet.factory.Assignments;
import org.ggp.base.util.propnet.factory.AssignmentsFactory;
import org.ggp.base.util.propnet.factory.AssignmentsImpl.ConstantForm;
import org.ggp.base.util.statemachine.Role;


public class ConstantFinder {
	/**
	 * Produces a new description of a game with time-invariant sentence forms
	 * removed, and an object for accessing the truth values of those sentences.
	 * This can be useful for state machines working with games that use
	 * sentences to describe things like the sum of two integers in the range
	 * [0, 100]. (This can crash many state machines.)
	 * @throws InterruptedException 
	 */
	public static ConstantChecker getConstants(List<Gdl> description) throws InterruptedException {
		description = DeORer.run(description);
		description = VariableConstrainer.replaceFunctionValuedVariables(description);
		
		return new ConstantChecker(description);
	}
	
	public static class ConstantChecker {
		List<Role> roles;
		Map<SentenceForm, Set<GdlSentence>> sentencesByForm = new HashMap<SentenceForm, Set<GdlSentence>>();
		SentenceModelImpl model;

		Map<SentenceForm, ConstantForm> constForms;
		
		public ConstantChecker(List<Gdl> description) throws InterruptedException {
			roles = Role.computeRoles(description);

			model = new SentenceModelImpl(description);
			for(SentenceForm form : model.getConstantSentenceForms()) {
				sentencesByForm.put(form, new HashSet<GdlSentence>());
			}
			//TODO: We want to restrict the domains in the model here to useful
			//values, but it's going to be tricky because the "useful values"
			//depend on what's needed in the remainder of the description
			//See: direction-from constants in mummymaze1p
			//Those take on values 0-50 when all they need are 0-8
			model.restrictDomainsToUsefulValues(null);
			//TODO: Now we need to actually use these restricted domains where applicable
			
			List<SentenceForm> ordering = getTopologicalOrdering(model.getConstantSentenceForms(), model.getDependencyGraph());
			constForms = new HashMap<SentenceForm, ConstantForm>();
			
			for(SentenceForm form : ordering) {
				Set<GdlRelation> relations = model.getRelations(form);
				Set<GdlRule> rules = model.getRules(form);
				addConstantSentenceForm(form, relations, rules);
				constForms.put(form, new ConstantForm(form, this));
			}
		}

		public ConstantChecker(ConstantChecker other) {
			roles = other.roles;
			sentencesByForm = new HashMap<SentenceForm, Set<GdlSentence>>();
			for(SentenceForm form : other.sentencesByForm.keySet()) {
				sentencesByForm.put(form, new HashSet<GdlSentence>(other.sentencesByForm.get(form)));
			}
			model = new SentenceModelImpl(other.model);
			constForms = new HashMap<SentenceForm, ConstantForm>(other.constForms);
		}

		private static List<SentenceForm> getTopologicalOrdering(
				Set<SentenceForm> forms,
				Map<SentenceForm, Set<SentenceForm>> dependencyGraph) throws InterruptedException {
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
				ConcurrencyUtils.checkForInterruption();
			}
			return ordering;
		}

		
		public List<Role> getRoles() {
			return roles;
		}

		public boolean isTrueConstant(GdlSentence sentence) {
			SentenceForm form = model.getSentenceForm(sentence);
			return sentencesByForm.get(form).contains(sentence);
		}
		public Iterator<GdlSentence> getTrueSentences(SentenceForm form) {
			Set<GdlSentence> sentences = sentencesByForm.get(form);
			if(sentences == null)
				return null;
			else
				return sentences.iterator();
		}

		public boolean hasConstantForm(GdlSentence sentence) {
			return SentenceModelUtils.inSentenceFormGroup(sentence, model.getConstantSentenceForms());
		}
		public boolean isConstantForm(SentenceForm form) {
			return sentencesByForm.containsKey(form);
		}

		public Iterator<List<GdlConstant>> getTrueTuples(SentenceForm form) {
			final SentenceForm finalForm = form;
			if(form == null)
				System.out.println("form is null");
			if(sentencesByForm.get(finalForm) == null)
				System.out.println("No set found for form " + form);
			return new Iterator<List<GdlConstant>>() {
				Iterator<GdlSentence> sentenceItr = sentencesByForm.get(finalForm).iterator();
				@Override
				public boolean hasNext() {
					return sentenceItr.hasNext();
				}
				@Override
				public List<GdlConstant> next() {
					return GdlUtils.getTupleFromGroundSentence(sentenceItr.next());
				}
				@Override
				public void remove() {
					//Unimplemented
				}
			};
		}
		
		private void addConstantSentenceForm(SentenceForm form,
				Set<GdlRelation> relations, Set<GdlRule> rules) throws InterruptedException {
			Set<GdlRule> nonRecursiveRules = new HashSet<GdlRule>();
			Set<GdlRule> recursiveRules = new HashSet<GdlRule>();

			for(GdlRule rule : rules) {
				boolean containsItself = false;
				for(GdlLiteral literal : rule.getBody())
					if(literal instanceof GdlRelation)
						if(form.matches((GdlSentence)literal))
							containsItself = true;
				if(containsItself)
					recursiveRules.add(rule);
				else
					nonRecursiveRules.add(rule);
			}

			Set<GdlSentence> trueByNonRecursives = new HashSet<GdlSentence>();
			trueByNonRecursives.addAll(relations);
			for(GdlRule rule : nonRecursiveRules) {
				Assignments assignments = AssignmentsFactory.getAssignmentsForRule(rule, model, constForms, sentencesByForm);
				GdlSentence head = rule.getHead();
				List<GdlVariable> varsInHead = GdlUtils.getVariables(head);

				AssignmentIterator asnItr = assignments.getIterator();
				while(asnItr.hasNext()) {
					ConcurrencyUtils.checkForInterruption();
					Map<GdlVariable, GdlConstant> assignment = asnItr.next();

					boolean isGoodAssignment = true;
					for(GdlLiteral literal : rule.getBody()) {
						if(literal instanceof GdlSentence) {
							GdlSentence transformed = CommonTransforms.replaceVariables((GdlSentence)literal, assignment);
							SentenceForm literalForm = model.getSentenceForm(transformed);
							
							if(!sentencesByForm.get(literalForm).contains(transformed)) {
								isGoodAssignment = false;
								List<GdlVariable> varsToChange = GdlUtils.getVariables(literal);
								asnItr.changeOneInNext(varsToChange, assignment);
							}
						} else if(literal instanceof GdlNot) {
							GdlSentence internal = (GdlSentence) ((GdlNot) literal).getBody();
							GdlSentence transformed = CommonTransforms.replaceVariables(internal, assignment);
							SentenceForm internalForm = model.getSentenceForm(transformed);
							
							if(sentencesByForm.get(internalForm).contains(transformed)) {
								isGoodAssignment = false;
								List<GdlVariable> varsToChange = GdlUtils.getVariables(literal);
								asnItr.changeOneInNext(varsToChange, assignment);
							}						
						} else if(literal instanceof GdlDistinct) {
							//Do nothing, handled in Assignments
						} else {
							throw new RuntimeException("Bad GdlLiteral type, probably OR");
						}
					}

					if(isGoodAssignment) {
						//Add it to the "good" list
						trueByNonRecursives.add(CommonTransforms.replaceVariables(head, assignment));
						//The constant head is a proposition? It comes up sometimes...
						//Try applying condensation isolation to Zhadu
						if(!(head instanceof GdlProposition))
							asnItr.changeOneInNext(varsInHead, assignment);
					} else {
						//Nothing to do, I guess...
					}
				}
			}
			
			ConcurrencyUtils.checkForInterruption();

			Set<GdlSentence> allTrueSentences = new HashSet<GdlSentence>(trueByNonRecursives);
			Set<GdlSentence> recentAdditions = new HashSet<GdlSentence>(trueByNonRecursives);
			Set<GdlSentence> newlyTrue = new HashSet<GdlSentence>();
			while(!recentAdditions.isEmpty()) {

				//Da da da, do rules
				for(GdlRule rule : recursiveRules) {
					for(GdlSentence input : recentAdditions) {
						ConcurrencyUtils.checkForInterruption();
						
						//TODO: Lack of inputs here seems to be causing slowdown
						//need constantForms, completedSentenceFormValues
						Assignments assignments = AssignmentsFactory.getAssignmentsWithRecursiveInput(rule, model, form, input, constForms, true, Collections.<SentenceForm, Collection<GdlSentence>>emptyMap()/*TODO sentencesByForm*/);
						GdlSentence head = rule.getHead();
						List<GdlVariable> varsInHead = GdlUtils.getVariables(head);

						AssignmentIterator asnItr = assignments.getIterator();
						while(asnItr.hasNext()) {
							Map<GdlVariable, GdlConstant> assignment = asnItr.next();
							//System.out.println(assignment);

							boolean isGoodAssignment = true;

							GdlSentence transformedHead = CommonTransforms.replaceVariables(head, assignment);
							if(allTrueSentences.contains(transformedHead)) {
								asnItr.changeOneInNext(varsInHead, assignment);
								isGoodAssignment = false;
							}

							for(GdlLiteral literal : rule.getBody()) {
								if(literal instanceof GdlSentence) {
									GdlSentence transformed = CommonTransforms.replaceVariables((GdlSentence)literal, assignment);
									SentenceForm literalForm = model.getSentenceForm(transformed);

									if(form.matches(transformed)) {
										//Works slightly differently for the recursive sentence form
										if(!allTrueSentences.contains(transformed)) {
											isGoodAssignment = false;
											List<GdlVariable> varsToChange = GdlUtils.getVariables(literal);
											asnItr.changeOneInNext(varsToChange, assignment);
										}
									} else {
										//Component conj = components.get(transformed);
										if(!sentencesByForm.get(literalForm).contains(transformed)) {
											isGoodAssignment = false;
											List<GdlVariable> varsToChange = GdlUtils.getVariables(literal);
											asnItr.changeOneInNext(varsToChange, assignment);
										}
									}
								} else if(literal instanceof GdlNot) {
									//We can't have the recursive case here, by GDL rules
									GdlSentence internal = (GdlSentence) ((GdlNot) literal).getBody();
									GdlSentence transformed = CommonTransforms.replaceVariables(internal, assignment);
									SentenceForm internalForm = model.getSentenceForm(transformed);
									//Component conj = components.get(transformed);
									if(sentencesByForm.get(internalForm).contains(transformed)) {
										isGoodAssignment = false;
										List<GdlVariable> varsToChange = GdlUtils.getVariables(literal);
										asnItr.changeOneInNext(varsToChange, assignment);
									}						
								} else if(literal instanceof GdlDistinct) {
									//Do nothing
								} else {
									throw new RuntimeException("Bad GdlLiteral type, probably OR");
								}
							}

							if(isGoodAssignment) {
								//Add it to the "good" list
								newlyTrue.add(CommonTransforms.replaceVariables(head, assignment));
								//asnItr.changeOneInNext(varsInHead, assignment);
								break; //
							} else {
								//Nothing to do, I guess...
							}

						}
					}
				}

				allTrueSentences.addAll(newlyTrue);
				recentAdditions.clear();
				recentAdditions.addAll(newlyTrue);
				newlyTrue.clear();
			}

			if(sentencesByForm.get(form) == null)
				sentencesByForm.put(form, new HashSet<GdlSentence>());
			sentencesByForm.get(form).addAll(allTrueSentences);
		}

		/**
		 * Returns all constant (time-independent) constant forms.
		 */
		public Set<SentenceForm> getSentenceForms() {
			return sentencesByForm.keySet();
		}

		public Integer getNumTrueTuples(SentenceForm form) {
			return sentencesByForm.get(form).size();
		}

		//Currently makes a lot of assumptions about how this will be used.
		//Basically custom-designed for the CondensationIsolator right now.
		@SuppressWarnings("unchecked")
		public void replaceRules(List<GdlRule> oldRules, List<GdlRule> newRules) throws InterruptedException {
			//See which of the new rules involve constants
			outer : for(GdlRule rule : newRules) {
				for(GdlLiteral literal : rule.getBody()) {
					GdlSentence sentence;
					if(literal instanceof GdlSentence) {
						sentence = (GdlSentence) literal;
					} else if(literal instanceof GdlNot) {
						sentence = (GdlSentence) ((GdlNot) literal).getBody();
					} else continue;
					if(!hasConstantForm(sentence))
						continue outer;
				}
				//It's a constant form
				GdlSentence head = rule.getHead();
				model.replaceRules(Collections.EMPTY_LIST, Collections.singletonList(rule));
				SentenceForm headForm = model.getSentenceForm(head);
				addConstantSentenceForm(headForm, Collections.EMPTY_SET, 
						Collections.singleton(rule));
				constForms.put(headForm, new ConstantForm(headForm, this));
			}
		}

		public Set<GdlConstant> getDomainInPossibleCompletions(
				GdlRelation relation, GdlVariable var) {
			//We want to figure out the values that the variable
			//var could take on in this relation
			Set<GdlConstant> domain = new HashSet<GdlConstant>();
			SentenceForm form = model.getSentenceForm(relation);
			//TODO: Divide into two cases, depending on if it's faster
			//to iterate over every true constant or iterate over
			//completions of the assignment
			//For now: Iterate over every constant
			//This is not efficient, just fast to code
			for(GdlSentence constant : sentencesByForm.get(form)) {
				Map<GdlVariable, GdlConstant> assignment = GdlUtils.getAssignmentMakingLeftIntoRight(relation, constant);
				if(assignment != null)
					domain.add(assignment.get(var));
			}
			return domain;
		}

		public SentenceModel getSentenceModel() {
			return model; //Gentleman's agreement to not modify
		}

		public Map<SentenceForm, ConstantForm> getConstantForms() throws InterruptedException {
			Map<SentenceForm, ConstantForm> result = new HashMap<SentenceForm, ConstantForm>();
			for(SentenceForm sf : getSentenceForms()) {
				result.put(sf, new ConstantForm(sf, this));
			}
			return result;
		}
	}

}
