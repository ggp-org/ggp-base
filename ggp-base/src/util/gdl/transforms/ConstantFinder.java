package util.gdl.transforms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlVariable;
import util.gdl.model.SentenceModel;
import util.gdl.model.SentenceModel.SentenceForm;
import util.propnet.factory.Assignments;
import util.propnet.factory.Assignments.AssignmentIterator;
import util.propnet.factory.Assignments.ConstantForm;
import util.statemachine.Role;

public class ConstantFinder {
	/**
	 * Produces a new description of a game with time-invariant sentence forms
	 * removed, and an object for accessing the truth values of those sentences.
	 * This can be useful for state machines working with games that use
	 * sentences to describe things like the sum of two integers in the range
	 * [0, 100]. (This can crash many state machines.)
	 */
	public static ConstantChecker getConstants(List<Gdl> description) {
		description = DeORer.run(description);
		description = VariableConstrainer.replaceFunctionValuedVariables(description);
		
		//Our first step will be to identify the sentence forms that are constant.
		SentenceModel model = new SentenceModel(description);
		Set<SentenceForm> changingForms = new HashSet<SentenceForm>(); 
		changingForms.addAll(model.getDependentSentenceForms());
		changingForms.addAll(model.getIndependentSentenceForms());
		changingForms.removeAll(model.getConstantSentenceForms());
		Set<SentenceForm> constantForms = model.getConstantSentenceForms();
		
		//Now we want to separate the description into two parts.
		List<Gdl> constantDescription = new ArrayList<Gdl>();
		List<Gdl> changingDescription = new ArrayList<Gdl>();
		for(Gdl gdl : description) {
			if(gdl instanceof GdlSentence) {
				GdlSentence sentence = (GdlSentence) gdl;
				if(matchesAny(sentence, constantForms))
					constantDescription.add(sentence);
				else
					changingDescription.add(sentence);
			} else if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				//Here we rely on the fact that if the sentence head is constant,
				//by definition the rest of the rule uses only constants.
				if(matchesAny(rule.getHead(), constantForms))
					constantDescription.add(rule);
				else
					changingDescription.add(rule);
			}
		}
		
		//System.out.println("Constant description:");
		//for(Gdl gdl : constantDescription)
		//	System.out.println(gdl);
		
		//Now we have the two descriptions.
		return new ConstantChecker(constantDescription, changingDescription);
	}
	
	private static boolean matchesAny(GdlSentence sentence,
			Set<SentenceForm> forms) {
		//TODO: We can probably speed this up by making "constantForms"
		//a map from sentence names to sentence forms. This probably
		//doesn't need to be sped up, though.
		
		for(SentenceForm form : forms) {
			if(form.matches(sentence))
				return true;
		}
		return false;
	}


	public static class ConstantChecker {
		//List<Gdl> changingDescription; //The remaining rules in the description
		List<Role> roles;
		Map<SentenceForm, Set<GdlSentence>> sentencesByForm = new HashMap<SentenceForm, Set<GdlSentence>>();
		SentenceModel model;
		
		public ConstantChecker(List<Gdl> constantDescription,
				List<Gdl> changingDescription) {
			//this.changingDescription = changingDescription;

			roles = Role.computeRoles(constantDescription);
			
			model = new SentenceModel(constantDescription, true);
			for(SentenceForm form : model.getSentenceForms()) {
				sentencesByForm.put(form, new HashSet<GdlSentence>());
			}
			
			List<SentenceForm> ordering = getTopologicalOrdering(model.getSentenceForms(), model.getDependencyGraph());
			Map<SentenceForm, ConstantForm> constForms = new HashMap<SentenceForm, ConstantForm>();
			
			for(SentenceForm form : ordering) {
				Set<GdlRelation> relations = model.getRelations(form);
				Set<GdlRule> rules = model.getRules(form);
				addConstantSentenceForm(form, relations, rules, constForms);
				constForms.put(form, new ConstantForm(form, this));
			}
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

		
		public List<Role> getRoles() {
			return roles;
		}

		/*public List<Gdl> getRemainingDescription() {
			return changingDescription;
		}*/
		
		public boolean isTrueConstant(GdlSentence sentence) {
			SentenceForm form = model.getSentenceForm(sentence);
			return sentencesByForm.get(form).contains(sentence);
		}
		public Iterator<GdlSentence> getTrueSentences(SentenceForm form) {
			return sentencesByForm.get(form).iterator();
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
					return SentenceModel.getTupleFromGroundSentence(sentenceItr.next());
				}
				@Override
				public void remove() {
					//Unimplemented
				}
			};
		}
		
		private void addConstantSentenceForm(SentenceForm form,
				Set<GdlRelation> relations, Set<GdlRule> rules,
				Map<SentenceForm, ConstantForm> constForms) {
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
				Assignments assignments = Assignments.getAssignmentsForRule(rule, model, constForms);
				GdlSentence head = rule.getHead();
				List<GdlVariable> varsInHead = getVarsInConjunct(head);

				AssignmentIterator asnItr = assignments.getIterator();
				while(asnItr.hasNext()) {
					Map<GdlVariable, GdlConstant> assignment = asnItr.next();

					boolean isGoodAssignment = true;
					for(GdlLiteral literal : rule.getBody()) {
						if(literal instanceof GdlSentence) {
							GdlSentence transformed = CommonTransforms.replaceVariables((GdlSentence)literal, assignment);
							SentenceForm literalForm = model.getSentenceForm(transformed);
							
							//Component conj = components.get(transformed);
							if(!sentencesByForm.get(literalForm).contains(transformed)) {
								isGoodAssignment = false;
								List<GdlVariable> varsToChange = getVarsInConjunct(literal);
								asnItr.changeOneInNext(varsToChange, assignment);
							}
						} else if(literal instanceof GdlNot) {
							GdlSentence internal = (GdlSentence) ((GdlNot) literal).getBody();
							GdlSentence transformed = CommonTransforms.replaceVariables(internal, assignment);
							//Component conj = components.get(transformed);
							SentenceForm internalForm = model.getSentenceForm(transformed);
							
							if(sentencesByForm.get(internalForm).contains(transformed)) {
								isGoodAssignment = false;
								List<GdlVariable> varsToChange = getVarsInConjunct(literal);
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

			Set<GdlSentence> allTrueSentences = new HashSet<GdlSentence>(trueByNonRecursives);
			Set<GdlSentence> recentAdditions = new HashSet<GdlSentence>(trueByNonRecursives);
			Set<GdlSentence> newlyTrue = new HashSet<GdlSentence>();
			while(!recentAdditions.isEmpty()) {

				//Da da da, do rules
				for(GdlRule rule : recursiveRules) {
					for(GdlSentence input : recentAdditions) {
						Assignments assignments = Assignments.getAssignmentsWithRecursiveInput(rule, model, form, input, null, false);
						GdlSentence head = rule.getHead();
						List<GdlVariable> varsInHead = getVarsInConjunct(head);

						AssignmentIterator asnItr = assignments.getIterator();
						while(asnItr.hasNext()) {
							Map<GdlVariable, GdlConstant> assignment = asnItr.next();

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
											List<GdlVariable> varsToChange = getVarsInConjunct(literal);
											asnItr.changeOneInNext(varsToChange, assignment);
										}
									} else {
										//Component conj = components.get(transformed);
										if(!sentencesByForm.get(literalForm).contains(transformed)) {
											isGoodAssignment = false;
											List<GdlVariable> varsToChange = getVarsInConjunct(literal);
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
										List<GdlVariable> varsToChange = getVarsInConjunct(literal);
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

			/*for(GdlSentence sentence : allTrueSentences) {
				sentencesByForm.get(form).add(sentence);
			}*/
			sentencesByForm.get(form).addAll(allTrueSentences);
		}

	}
	
	private static List<GdlVariable> getVarsInConjunct(GdlLiteral literal) {
		return SentenceModel.getVariables(literal);
	}
	
}
