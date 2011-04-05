package util.propnet.factory;

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
import java.util.Map.Entry;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;
import util.gdl.model.SentenceModel;
import util.gdl.model.SentenceModel.SentenceForm;
import util.gdl.transforms.CommonTransforms;
import util.gdl.transforms.Relationizer;
import util.gdl.transforms.SimpleCondensationIsolator;
import util.gdl.transforms.CrudeSplitter;
import util.gdl.transforms.DeORer;
import util.gdl.transforms.GdlCleaner;
import util.gdl.transforms.CondensationIsolator;
import util.gdl.transforms.ConstantFinder;
import util.gdl.transforms.VariableConstrainer;
import util.gdl.transforms.ConstantFinder.ConstantChecker;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import util.propnet.factory.Assignments.AssignmentIterator;
import util.propnet.factory.Assignments.ConstantForm;
import util.statemachine.Role;

/*
 * A propnet factory meant to optimize the propnet before it's even built,
 * mostly through transforming the GDL. (The transformations identify certain
 * classes of rules that have poor performance and replace them with equivalent
 * rules that have better performance, with performance measured by the size of
 * the propnet.)
 * 
 * Known issues:
 * - Does not work on games with many advanced forms of recursion. These include:
 *   - Anything that breaks the SentenceModel
 *   - Multiple sentence forms which reference one another in rules
 *   - Not 100% confirmed to work on games where recursive rules have multiple
 *     recursive conjuncts
 * - Currently runs some of the transformations multiple times. A Description
 *   object containing information about the description and its properties would
 *   alleviate this.
 * - Its current solution to the "unaffected piece rule" problem is somewhat
 *   clumsy and ungeneralized, relying on the combined behaviors of CrudeSplitter
 *   and CondensationIsolator.
 *   - The mutex finder in particular is very ungeneralized. It should be replaced
 *     with a more general mutex finder.
 * - Depending on the settings and the situation, the behavior of the
 *   CondensationIsolator can be either too aggressive or not aggressive enough.
 *   Both result in excessively large games. A more sophisticated version of the
 *   CondensationIsolator could solve these problems. A stopgap alternative is to
 *   try both settings and use the smaller propnet (or the first to be created,
 *   if multithreading).
 * 
 */
public class OptimizingPropNetFactory {
	static final private GdlConstant LEGAL = GdlPool.getConstant("legal");
	static final private GdlConstant NEXT = GdlPool.getConstant("next");
	static final private GdlConstant TRUE = GdlPool.getConstant("true");
	static final private GdlConstant DOES = GdlPool.getConstant("does");
	static final private GdlConstant GOAL = GdlPool.getConstant("goal");
	static final private GdlConstant INIT = GdlPool.getConstant("init");
	static final private GdlConstant INIT_CAPS = GdlPool.getConstant("INIT");
	@SuppressWarnings("unused")
    static final private GdlConstant BASE = GdlPool.getConstant("base");
	@SuppressWarnings("unused")
    static final private GdlConstant INPUT = GdlPool.getConstant("input");
	static final private GdlConstant TEMP = GdlPool.getConstant("TEMP");

	public static PropNet create(List<Gdl> description) {
		return create(description, false);
	}
	
	//These heuristic methods work best on the vast majority of games.
	//Still problems with conn4, mummyMaze2p_2007, sudoku2;
	// possibly others?
	public static PropNet create(List<Gdl> description, boolean verbose) {
		return create(description, verbose, true, false, false, false, true, true);
	}
	
	public static PropNet create(List<Gdl> description,
			boolean verbose,
			boolean useAdvancedCondensers,
			boolean moreRestraint,
			boolean useCrudeSplitter,
			boolean constConstraint,
			boolean useHeuristic,
			boolean analyticFunctionOrdering)
	{
		System.out.println("Building propnet...");

		long startTime = System.currentTimeMillis();

		description = GdlCleaner.run(description);
		description = DeORer.run(description);
		description = VariableConstrainer.replaceFunctionValuedVariables(description);
		description = Relationizer.run(description);

		if(useCrudeSplitter) {
			description = CrudeSplitter.run(description);
			description = CondensationIsolator.run(description, true, true, true, constConstraint, useHeuristic, analyticFunctionOrdering);
		} else {
			if(!useAdvancedCondensers)
				description = SimpleCondensationIsolator.run(description, false);
			if(useAdvancedCondensers)
				description = CondensationIsolator.run(description, true, moreRestraint, true, constConstraint, useHeuristic, analyticFunctionOrdering);
		}
		if(verbose)
			for(Gdl gdl : description)
				System.out.println(gdl);

		//We want to start with a rule graph and follow the rule graph.
		//Start with the constants, etc.
		SentenceModel model = new SentenceModel(description);

		//Restrict domains to values that could actually come up in rules.
		//See chinesecheckers4's "count" relation for an example of why this
		//could be useful. In most situations, this has no effect.
		model.restrictDomainsToUsefulValues();

		boolean usingBase = model.getSentenceNames().contains("base");
		boolean usingInput = model.getSentenceNames().contains("input");

		//Is this a good place to get the constants?
		if(verbose)
			System.out.println("Setting constants...");
		ConstantChecker constantChecker = ConstantFinder.getConstants(description);
		if(verbose)
			System.out.println("Done setting constants");
		
		//For now, we're going to build this to work on those with a
		//particular restriction on the dependency graph:
		//Recursive loops may only contain one sentence form.
		//This describes most games, but not all legal games.
		Map<SentenceForm, Set<SentenceForm>> dependencyGraph = model.getDependencyGraph();
		if(verbose) {
			System.out.print("Computing topological ordering... ");
			System.out.flush();
		}
		List<SentenceForm> topologicalOrdering = getTopologicalOrdering(model.getSentenceForms(), dependencyGraph, usingBase, usingInput);
		if(verbose)
			System.out.println("done");
		//Now what?
		//PropNet propnet = new PropNet(); This is actually the last step
		List<Role> roles = Role.computeRoles(description);
		Map<GdlSentence, Component> components = new HashMap<GdlSentence, Component>();
		Map<GdlSentence, Component> negations = new HashMap<GdlSentence, Component>();
		Constant trueComponent = new Constant(true);
		Constant falseComponent = new Constant(false);
		Map<SentenceForm, ConstantForm> constantForms = new HashMap<SentenceForm, ConstantForm>();
		Map<SentenceForm, Collection<GdlSentence>> completedSentenceFormValues = new HashMap<SentenceForm, Collection<GdlSentence>>();
		for(SentenceForm form : topologicalOrdering) {
			if(verbose) {
				System.out.print("Adding sentence form " + form);
				System.out.flush();
			}
			if(constantChecker.isConstantForm(form)) {
				if(verbose)
					System.out.println(" (constant)");
				//Only add it if it's important
				if(form.getName().equals(LEGAL)
						|| form.getName().equals(GOAL)
						|| form.getName().equals(INIT)) {
					//Add it
					Iterator<GdlSentence> sentenceItr = constantChecker.getTrueSentences(form);
					if(!sentenceItr.hasNext())
						System.out.println("Empty sentence iterator");
					while(sentenceItr.hasNext()) {
						GdlSentence trueSentence = sentenceItr.next();
						//System.out.println("Adding prop for sentence " + trueSentence);
						Proposition trueProp = new Proposition(trueSentence.toTerm());
						trueProp.addInput(trueComponent);
						trueComponent.addOutput(trueProp);
						//components.put(trueSentence, trueProp);
						components.put(trueSentence, trueComponent);
					}
				}

				if(verbose)
					System.out.println("Checking whether " + form + " is a functional constant...");
				addToConstants(form, constantChecker, constantForms);
				addFormToCompletedValues(form, completedSentenceFormValues, constantChecker);
				
				continue;
			}
			if(verbose)
				System.out.println();
			//TODO: Adjust "recursive forms" appropriately
			//Add a temporary sentence form thingy? ...
			Map<GdlSentence, Component> temporaryComponents = new HashMap<GdlSentence, Component>();
			Map<GdlSentence, Component> temporaryNegations = new HashMap<GdlSentence, Component>();
			addSentenceForm(form, model, description, components, negations, trueComponent, falseComponent, usingBase, usingInput, Collections.singleton(form), temporaryComponents, temporaryNegations, constantForms, constantChecker, completedSentenceFormValues);
			//TODO: Pass these over groups of multiple sentence forms
			if(verbose && !temporaryComponents.isEmpty())
				System.out.println("Processing temporary components...");
			processTemporaryComponents(temporaryComponents, temporaryNegations, components, negations, trueComponent, falseComponent);
			addFormToCompletedValues(form, completedSentenceFormValues, components);
			//if(verbose)
				//TODO: Add this, but with the correct total number of components (not just Propositions)
				//System.out.println("  "+completedSentenceFormValues.get(form).size() + " components added");
		}
		//Connect "next" to "true"
		if(verbose)
			System.out.println("Adding transitions...");
		addTransitions(components);
		//Set up "init" proposition
		if(verbose)
			System.out.println("Setting up 'init' proposition...");
		setUpInit(components, trueComponent, falseComponent, constantChecker);
		if(verbose)
			System.out.println("Creating component set...");
		Set<Component> componentSet = new HashSet<Component>(components.values());
		//Try saving some memory here...
		components = null;
		negations = null;
		completeComponentSet(componentSet);
		if(verbose)
			System.out.println("Initializing propnet object...");
		//Make it look the same as the PropNetFactory results, until we decide
		//how we want it to look
		normalizePropositions(componentSet);
		PropNet propnet = new PropNet(roles, componentSet);
		if(verbose) {
			System.out.println("Done setting up propnet; took " + (System.currentTimeMillis() - startTime) + "ms, has " + componentSet.size() + " components and " + propnet.getNumLinks() + " links");
			System.out.println("Propnet has " +propnet.getNumAnds()+" ands; "+propnet.getNumOrs()+" ors; "+propnet.getNumNots()+" nots");
		}
		//System.out.println(propnet);
		return propnet;
	}

	/**
	 * Changes the propositions contained in the propnet so that they correspond
	 * to the outputs of the PropNetFactory. This is for consistency and for
	 * backwards compatibility with respect to state machines designed for the
	 * old propnet factory. Feel free to remove this for your player.
	 * 
	 * @param componentSet
	 */
	private static void normalizePropositions(Set<Component> componentSet) {
		for(Component component : componentSet) {
			if(component instanceof Proposition) {
				Proposition p = (Proposition) component;
				GdlTerm sentenceAsTerm = p.getName();
				if(sentenceAsTerm instanceof GdlFunction) {
					GdlFunction sentenceAsFunction = (GdlFunction) sentenceAsTerm;
					if(sentenceAsFunction.getName().equals(NEXT)) {
						p.setName(GdlPool.getConstant("anon"));
					} else if(sentenceAsFunction.getName().equals(TRUE)) {
						p.setName(sentenceAsFunction.get(0));
					}
				}
			}
		}
	}

	private static void addFormToCompletedValues(
			SentenceForm form,
			Map<SentenceForm, Collection<GdlSentence>> completedSentenceFormValues,
			ConstantChecker constantChecker) {
		constantChecker.getTrueSentences(form);
		List<GdlSentence> sentences = new ArrayList<GdlSentence>();
		Iterator<GdlSentence> itr = constantChecker.getTrueSentences(form);
		while(itr.hasNext())
			sentences.add(itr.next());

		completedSentenceFormValues.put(form, sentences);
	}


	private static void addFormToCompletedValues(
			SentenceForm form,
			Map<SentenceForm, Collection<GdlSentence>> completedSentenceFormValues,
			Map<GdlSentence, Component> components) {
		//Kind of inefficient. Could do better by collecting these as we go,
		//then adding them back into the CSFV map once the sentence forms are complete.
		//completedSentenceFormValues.put(form, new ArrayList<GdlSentence>());
		List<GdlSentence> sentences = new ArrayList<GdlSentence>();
		for(GdlSentence sentence : components.keySet()) {
			if(form.matches(sentence)) {
				//The sentence has a node associated with it
				sentences.add(sentence);
			}
		}
		completedSentenceFormValues.put(form, sentences);
	}


	private static void addToConstants(SentenceForm form,
			ConstantChecker constantChecker, Map<SentenceForm, ConstantForm> constantForms) {
		constantForms.put(form, new ConstantForm(form, constantChecker));
	}


	private static void processTemporaryComponents(
			Map<GdlSentence, Component> temporaryComponents,
			Map<GdlSentence, Component> temporaryNegations,
			Map<GdlSentence, Component> components,
			Map<GdlSentence, Component> negations, Component trueComponent,
			Component falseComponent) {
		//For each component in temporary components, we want to "put it back"
		//into the main components section.
		//We also want to do optimization here...
		//We don't want to end up with anything following from true/false.

		//Everything following from a temporary component (its outputs)
		//should instead become an output of the actual component.
		//If there is no actual component generated, then the statement
		//is necessarily FALSE and should be replaced by the false
		//component.
		for(GdlSentence sentence : temporaryComponents.keySet()) {
			Component tempComp = temporaryComponents.get(sentence);
			Component realComp = components.get(sentence);
			if(realComp == null) {
				realComp = falseComponent;
			}
			for(Component output : tempComp.getOutputs()) {
				//Disconnect
				output.removeInput(tempComp);
				//tempComp.removeOutput(output); //do at end
				//Connect
				output.addInput(realComp);
				realComp.addOutput(output);
			}
			tempComp.removeAllOutputs();

			if(temporaryNegations.containsKey(sentence)) {
				//Should be pointing to a "not" that now gets input from realComp
				//Should be fine to put into negations
				negations.put(sentence, temporaryNegations.get(sentence));
				//If this follows true/false, will get resolved by the next set of optimizations
			}

			while(hasNonessentialChildren(trueComponent) || hasNonessentialChildren(falseComponent)) {
				optimizeAwayTrue(components, negations, trueComponent, falseComponent);
				optimizeAwayFalse(components, negations, trueComponent, falseComponent);
			}

		}
	}


	private static void optimizeAwayFalse(
			Map<GdlSentence, Component> components, Map<GdlSentence, Component> negations, Component trueComponent,
			Component falseComponent) {
		while(hasNonessentialChildren(falseComponent)) {
			Iterator<Component> outputItr = falseComponent.getOutputs().iterator();
			Component output = outputItr.next();
			while(isEssentialProposition(output) || output instanceof Transition)
				output = outputItr.next();
			if(output instanceof Proposition) {
				//Move its outputs to be outputs of false
				for(Component child : output.getOutputs()) {
					//Disconnect
					child.removeInput(output);
					//output.removeOutput(child); //do at end
					//Reconnect; will get children before returning, if nonessential
					falseComponent.addOutput(child);
					child.addInput(falseComponent);
				}
				output.removeAllOutputs();

				if(!isEssentialProposition(output)) {
					Proposition prop = (Proposition) output;
					//Remove the proposition entirely
					falseComponent.removeOutput(output);
					output.removeInput(falseComponent);
					//Update its location to the trueComponent in our map
					components.put(prop.getName().toSentence(), falseComponent);
					negations.put(prop.getName().toSentence(), trueComponent);
				}
			} else if(output instanceof And) {
				And and = (And) output;
				//Attach children of and to falseComponent
				for(Component child : and.getOutputs()) {
					child.addInput(falseComponent);
					falseComponent.addOutput(child);
					child.removeInput(and);
				}
				//Disconnect and completely
				and.removeAllOutputs();
				for(Component parent : and.getInputs())
					parent.removeOutput(and);
				and.removeAllInputs();
			} else if(output instanceof Or) {
				Or or = (Or) output;
				//Remove as input from or
				or.removeInput(falseComponent);
				falseComponent.removeOutput(or);
				//If or has only one input, remove it
				if(or.getInputs().size() == 1) {
					Component in = or.getSingleInput();
					or.removeInput(in);
					in.removeOutput(or);
					for(Component out : or.getOutputs()) {
						//Disconnect from and
						out.removeInput(or);
						//or.removeOutput(out); //do at end
						//Connect directly to the new input
						out.addInput(in);
						in.addOutput(out);
					}
					or.removeAllOutputs();
				}
			} else if(output instanceof Not) {
				Not not = (Not) output;
				//Disconnect from falseComponent
				not.removeInput(falseComponent);
				falseComponent.removeOutput(not);
				//Connect all children of the not to trueComponent
				for(Component child : not.getOutputs()) {
					//Disconnect
					child.removeInput(not);
					//not.removeOutput(child); //Do at end
					//Connect to trueComponent
					child.addInput(trueComponent);
					trueComponent.addOutput(child);
				}
				not.removeAllOutputs();
			} else if(output instanceof Transition) {
				//???
				System.err.println("Fix optimizeAwayFalse's case for Transitions");
			}
		}		
	}


	private static void optimizeAwayTrue(
			Map<GdlSentence, Component> components, Map<GdlSentence, Component> negations, Component trueComponent,
			Component falseComponent) {
		while(hasNonessentialChildren(trueComponent)) {
			Iterator<Component> outputItr = trueComponent.getOutputs().iterator();
			Component output = outputItr.next();
			while(isEssentialProposition(output) || output instanceof Transition)
				output = outputItr.next();
			if(output instanceof Proposition) {
				//Move its outputs to be outputs of true
				for(Component child : output.getOutputs()) {
					//Disconnect
					child.removeInput(output);
					//output.removeOutput(child); //do at end
					//Reconnect; will get children before returning, if nonessential
					trueComponent.addOutput(child);
					child.addInput(trueComponent);
				}
				output.removeAllOutputs();

				if(!isEssentialProposition(output)) {
					Proposition prop = (Proposition) output;
					//Remove the proposition entirely
					trueComponent.removeOutput(output);
					output.removeInput(trueComponent);
					//Update its location to the trueComponent in our map
					components.put(prop.getName().toSentence(), trueComponent);
					negations.put(prop.getName().toSentence(), falseComponent);
				}
			} else if(output instanceof Or) {
				Or or = (Or) output;
				//Attach children of or to trueComponent
				for(Component child : or.getOutputs()) {
					child.addInput(trueComponent);
					trueComponent.addOutput(child);
					child.removeInput(or);
				}
				//Disconnect or completely
				or.removeAllOutputs();
				for(Component parent : or.getInputs())
					parent.removeOutput(or);
				or.removeAllInputs();
			} else if(output instanceof And) {
				And and = (And) output;
				//Remove as input from and
				and.removeInput(trueComponent);
				trueComponent.removeOutput(and);
				//If and has only one input, remove it
				if(and.getInputs().size() == 1) {
					Component in = and.getSingleInput();
					and.removeInput(in);
					in.removeOutput(and);
					for(Component out : and.getOutputs()) {
						//Disconnect from and
						out.removeInput(and);
						//and.removeOutput(out); //do at end
						//Connect directly to the new input
						out.addInput(in);
						in.addOutput(out);
					}
					and.removeAllOutputs();
				}
			} else if(output instanceof Not) {
				Not not = (Not) output;
				//Disconnect from trueComponent
				not.removeInput(trueComponent);
				trueComponent.removeOutput(not);
				//Connect all children of the not to falseComponent
				for(Component child : not.getOutputs()) {
					//Disconnect
					child.removeInput(not);
					//not.removeOutput(child); //Do at end
					//Connect to falseComponent
					child.addInput(falseComponent);
					falseComponent.addOutput(child);
				}
				not.removeAllOutputs();
			} else if(output instanceof Transition) {
				//???
				System.err.println("Fix optimizeAwayTrue's case for Transitions");
			}
		}
	}


	private static boolean hasNonessentialChildren(Component trueComponent) {
		for(Component child : trueComponent.getOutputs()) {
			if(child instanceof Transition)
				continue;
			if(!isEssentialProposition(child))
				return true;
			//We don't want any grandchildren, either
			if(!child.getOutputs().isEmpty())
				return true;
		}
		return false;
	}


	private static boolean isEssentialProposition(Component component) {
		if(!(component instanceof Proposition))
			return false;

		//We're looking for things that would be outputs of "true" or "false",
		//but we would still want to keep as propositions to be read by the
		//state machine ("init" is handled separately)
		Proposition prop = (Proposition) component;
		GdlConstant name = prop.getName().toSentence().getName();

		return (name.equals(LEGAL) || name.equals(NEXT) || name.equals(GOAL));
	}


	private static void completeComponentSet(Set<Component> componentSet) {
		Set<Component> newComponents = new HashSet<Component>();
		Set<Component> componentsToTry = new HashSet<Component>(componentSet);
		while(!componentsToTry.isEmpty()) {
			for(Component c : componentsToTry) {
				for(Component out : c.getOutputs()) {
					if(!componentSet.contains(out))
						newComponents.add(out);
				}
				for(Component in : c.getInputs()) {
					if(!componentSet.contains(in))
						newComponents.add(in);
				}
			}
			componentSet.addAll(newComponents);
			componentsToTry = newComponents;
			newComponents = new HashSet<Component>();
		}
	}


	private static void addTransitions(Map<GdlSentence, Component> components) {
		for(Entry<GdlSentence, Component> entry : components.entrySet()) {
			GdlSentence sentence = entry.getKey();

			if(sentence.getName().equals(NEXT)) {
				//connect to true
				GdlSentence trueSentence = GdlPool.getRelation(TRUE, sentence.getBody());
				Component nextComponent = entry.getValue();
				Component trueComponent = components.get(trueSentence);
				Transition transition = new Transition();
				transition.addInput(nextComponent);
				nextComponent.addOutput(transition);
				transition.addOutput(trueComponent);
				if(trueComponent == null)
					System.out.println(trueSentence);
				trueComponent.addInput(transition);
			}
		}
	}

	//TODO: Replace with version using constantChecker only
	private static void setUpInit(Map<GdlSentence, Component> components,
			Constant trueComponent, Constant falseComponent,
			ConstantChecker constantChecker) {
		Proposition initProposition = new Proposition(INIT_CAPS);
		for(Entry<GdlSentence, Component> entry : components.entrySet()) {
			//Is this something that will be true?
			if(entry.getValue() == trueComponent) {
				if(entry.getKey().getName().equals(INIT)) {
					//Find the corresponding true sentence
					GdlSentence trueSentence = GdlPool.getRelation(TRUE, entry.getKey().getBody());
					//System.out.println("True sentence from init: " + trueSentence);
					Component trueSentenceComponent = components.get(trueSentence);
					if(trueSentenceComponent.getInputs().isEmpty()) {
						//Case where there is no transition input
						//Add the transition input, connect to init, continue loop
						Transition transition = new Transition();
						//init goes into transition
						transition.addInput(initProposition);
						initProposition.addOutput(transition);
						//transition goes into component
						trueSentenceComponent.addInput(transition);
						transition.addOutput(trueSentenceComponent);
					} else {
						//The transition already exists
						Component transition = trueSentenceComponent.getSingleInput();

						//We want to add init as a thing that precedes the transition
						//Disconnect existing input
						Component input = transition.getSingleInput();
						//input and init go into or, or goes into transition
						input.removeOutput(transition);
						transition.removeInput(input);
						List<Component> orInputs = new ArrayList<Component>(2);
						orInputs.add(input);
						orInputs.add(initProposition);
						orify(orInputs, transition, falseComponent);
					}
				}
			}
		}
	}

	/**
	 * Adds an or gate connecting the inputs to produce the output.
	 * Handles special optimization cases like a true/false input.
	 */
	private static void orify(Collection<Component> inputs, Component output, Constant falseProp) {
		//TODO: Look for already-existing ors with the same inputs?
		//Or can this be handled with a GDL transformation?

		//Special case: An input is the true constant
		for(Component in : inputs) {
			if(in instanceof Constant && in.getValue()) {
				//True constant: connect that to the component, done
				in.addOutput(output);
				output.addInput(in);
				return;
			}		
		}

		//Special case: An input is "or"
		//I'm honestly not sure how to handle special cases here...
		//What if that "or" gate has multiple outputs? Could that happen?

		//For reals... just skip over any false constants
		Or or = new Or();
		for(Component in : inputs) {
			if(!(in instanceof Constant)) {
				in.addOutput(or);
				or.addInput(in);
			}
		}
		//What if they're all false? (Or inputs is empty?) Then no inputs at this point...
		if(or.getInputs().isEmpty()) {
			//Hook up to "false"
			falseProp.addOutput(output);
			output.addInput(falseProp);
			return;
		}
		//If there's just one, on the other hand, don't use the or gate
		if(or.getInputs().size() == 1) {
			Component in = or.getSingleInput();
			in.removeOutput(or);
			or.removeInput(in);
			in.addOutput(output);
			output.addInput(in);
			return;
		}
		or.addOutput(output);
		output.addInput(or);
	}

	//TODO: This code is currently used by multiple classes, so perhaps it should be
	//factored out into the SentenceModel.
	private static List<SentenceForm> getTopologicalOrdering(
			Set<SentenceForm> forms,
			Map<SentenceForm, Set<SentenceForm>> dependencyGraph, boolean usingBase, boolean usingInput) {
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
			//Don't add if it's true/next/legal/does and we're waiting for base/input
			if(usingBase && (curForm.getName().equals(TRUE) || curForm.getName().equals(NEXT) || curForm.getName().equals(INIT))) {
				//Have we added the corresponding base sf yet?
				SentenceForm baseForm = curForm.getCopyWithName("base");
				if(!alreadyOrdered.contains(baseForm)) {
					readyToAdd = false;
				}
			}
			if(usingInput && (curForm.getName().equals(DOES) || curForm.getName().equals(LEGAL))) {
				SentenceForm inputForm = curForm.getCopyWithName("input");
				if(!alreadyOrdered.contains(inputForm)) {
					readyToAdd = false;
				}
			}				
			//Add it
			if(readyToAdd) {
				ordering.add(curForm);
				alreadyOrdered.add(curForm);
			} else {
				queue.add(curForm);
			}
			//TODO: Add check for an infinite loop here, or stratify loops
		}
		return ordering;
	}

	private static void addSentenceForm(SentenceForm form, SentenceModel model,
			List<Gdl> description, Map<GdlSentence, Component> components,
			Map<GdlSentence, Component> negations,
			Constant trueComponent, Constant falseComponent,
			boolean usingBase, boolean usingInput,
			Set<SentenceForm> recursionForms,
			Map<GdlSentence, Component> temporaryComponents, Map<GdlSentence, Component> temporaryNegations,
			Map<SentenceForm, ConstantForm> constantForms, ConstantChecker constantChecker,
			Map<SentenceForm, Collection<GdlSentence>> completedSentenceFormValues) {
		//This is the meat of it (along with the entire Assignments class).
		//We need to enumerate the possible propositions in the sentence form...
		//We also need to hook up the sentence form to the inputs that can make it true.
		//We also try to optimize as we go, which means possibly removing the
		//proposition if it isn't actually possible, or replacing it with
		//true/false if it's a constant.

		Set<GdlRelation> relations = model.getRelations(form);
		Set<GdlRule> rules = model.getRules(form);

		for(GdlRelation relation : relations) {
			//We add the sentence as a constant
			if(relation.getName().equals(LEGAL)
					|| relation.getName().equals(NEXT)
					|| relation.getName().equals(GOAL)) {
				Proposition prop = new Proposition(relation.toTerm());
				//Attach to true
				trueComponent.addOutput(prop);
				prop.addInput(trueComponent);
				//Still want the same components;
				//we just don't want this to be anonymized
			}
			//Assign as true
			components.put(relation, trueComponent);
			negations.put(relation, falseComponent);
			continue;
		}

		//For does/true, make nodes based on input/base, if available
		if(usingInput && form.getName().equals(DOES)) {
			//Add only those propositions for which there is a corresponding INPUT
			SentenceForm inputForm = form.getCopyWithName("input");
			Iterator<GdlSentence> itr = constantChecker.getTrueSentences(inputForm);
			while(itr.hasNext()) {
				GdlSentence inputSentence = itr.next();
				GdlSentence doesSentence = GdlPool.getRelation(DOES, inputSentence.getBody());
				Proposition prop = new Proposition(doesSentence.toTerm());
				components.put(doesSentence, prop);
			}
			return;
		}
		if(usingBase && form.getName().equals(TRUE)) {
			SentenceForm baseForm = form.getCopyWithName("base");
			Iterator<GdlSentence> itr = constantChecker.getTrueSentences(baseForm);
			while(itr.hasNext()) {
				GdlSentence baseSentence = itr.next();
				GdlSentence trueSentence = GdlPool.getRelation(TRUE, baseSentence.getBody());
				Proposition prop = new Proposition(trueSentence.toTerm());
				components.put(trueSentence, prop);
			}
			return;
		}
		
		Map<GdlSentence, Set<Component>> inputsToOr = new HashMap<GdlSentence, Set<Component>>();
		for(GdlRule rule : rules) {
			Assignments assignments = Assignments.getAssignmentsForRule(rule, model, constantForms, completedSentenceFormValues);

			//Calculate vars in live (non-constant, non-distinct) conjuncts
			Set<GdlVariable> varsInLiveConjuncts = getVarsInLiveConjuncts(rule, constantChecker.getSentenceForms());
			varsInLiveConjuncts.addAll(SentenceModel.getVariables(rule.getHead()));
			Set<GdlVariable> varsInRule = new HashSet<GdlVariable>(SentenceModel.getVariables(rule));
			boolean preventDuplicatesFromConstants = 
				(varsInRule.size() > varsInLiveConjuncts.size());
			
			//System.out.println("Rule: " + rule);
			//Do we just pass those to the Assignments class in that case?
			for(AssignmentIterator asnItr = assignments.getIterator(); asnItr.hasNext(); ) {
				Map<GdlVariable, GdlConstant> assignment = asnItr.next();
				if(assignment == null) continue; //Not sure if this will ever happen

				GdlSentence sentence = CommonTransforms.replaceVariables(rule.getHead(), assignment);

				//Now we go through the conjuncts as before, but we wait to hook them up.
				List<Component> componentsToConnect = new ArrayList<Component>(rule.arity());
				for(GdlLiteral literal : rule.getBody()) {
					if(literal instanceof GdlSentence) {
						//Get the sentence post-substitutions
						GdlSentence transformed = CommonTransforms.replaceVariables((GdlSentence) literal, assignment);
						
						//Check for constant-ness
						SentenceForm conjunctForm = model.getSentenceForm(transformed);
						if(constantChecker.isConstantForm(conjunctForm)) {
							if(!constantChecker.isTrueConstant(transformed)) {
								List<GdlVariable> varsToChange = getVarsInConjunct(literal);
								asnItr.changeOneInNext(varsToChange, assignment);
								componentsToConnect.add(null);
							}
							continue;
						}
						
						Component conj = components.get(transformed);
						//If conj is null and this is a sentence form we're still handling,
						//hook up to a temporary sentence form
						if(conj == null) {
							conj = temporaryComponents.get(transformed);
						}
						if(conj == null && SentenceModel.inSentenceFormGroup(transformed, recursionForms)) {
							//Set up a temporary component
							Proposition tempProp = new Proposition(transformed.toTerm());
							temporaryComponents.put(transformed, tempProp);
							conj = tempProp;
						}
						//Let's say this is false; we want to backtrack and change the right variable
						if(conj == null || isThisConstant(conj, falseComponent)) {
							List<GdlVariable> varsInConjunct = getVarsInConjunct(literal);
							asnItr.changeOneInNext(varsInConjunct, assignment);
							//These last steps just speed up the process
							//telling the factory to ignore this rule
							componentsToConnect.add(null);
							continue; //look at all the other restrictions we'll face
						}

						componentsToConnect.add(conj);
					} else if(literal instanceof GdlNot) {
						//Add a "not" if necessary
						//Look up the negation
						GdlSentence internal = (GdlSentence) ((GdlNot) literal).getBody();
						GdlSentence transformed = CommonTransforms.replaceVariables(internal, assignment);
						
						//Add constant-checking here...
						SentenceForm conjunctForm = model.getSentenceForm(transformed);
						if(constantChecker.isConstantForm(conjunctForm)) {
							if(constantChecker.isTrueConstant(transformed)) {
								List<GdlVariable> varsToChange = getVarsInConjunct(literal);
								asnItr.changeOneInNext(varsToChange, assignment);
								componentsToConnect.add(null);
							}
							continue;
						}
						
						Component conj = negations.get(transformed);
						if(isThisConstant(conj, falseComponent)) {
							//We need to change one of the variables inside
							List<GdlVariable> varsInConjunct = getVarsInConjunct(internal);
							asnItr.changeOneInNext(varsInConjunct, assignment);
							//ignore this rule
							componentsToConnect.add(null);
							continue;
						}
						if(conj == null) {
							conj = temporaryNegations.get(transformed);
						}
						//Check for the recursive case:
						if(conj == null && SentenceModel.inSentenceFormGroup(transformed, recursionForms)) {
							Component positive = components.get(transformed);
							if(positive == null) {
								positive = temporaryComponents.get(transformed);
							}
							if(positive == null) {
								//Make the temporary proposition
								Proposition tempProp = new Proposition(transformed.toTerm());
								temporaryComponents.put(transformed, tempProp);
								positive = tempProp;
							}
							//Positive is now set and in temporaryComponents
							//Evidently, wasn't in temporaryNegations
							//So we add the "not" gate and set it in temporaryNegations
							Not not = new Not();
							//Add positive as input
							not.addInput(positive);
							positive.addOutput(not);
							temporaryNegations.put(transformed, not);
							conj = not;
						}
						if(conj == null) {
							Component positive = components.get(transformed);
							//No, because then that will be attached to "negations", which could be bad

							if(positive == null) {
								//So the positive can't possibly be true (unless we have recurstion)
								//and so this would be positive always
								//We want to just skip this conjunct, so we continue to the next

								continue; //to the next conjunct
							}

							//Check if we're sharing a component with another sentence with a negation
							//(i.e. look for "nots" in our outputs and use those instead)
							Not existingNotOutput = getNotOutput(positive);
							if(existingNotOutput != null) {
								componentsToConnect.add(existingNotOutput);
								negations.put(transformed, existingNotOutput);
								continue; //to the next conjunct
							}

							Not not = new Not();
							not.addInput(positive);
							positive.addOutput(not);
							negations.put(transformed, not);
							conj = not;
						}
						if(conj == null)
							System.out.println("null case with negated sentence " + transformed);
						componentsToConnect.add(conj);
					} else if(literal instanceof GdlDistinct) {
						//Already handled; ignore
					} else {
						throw new RuntimeException("Unwanted GdlLiteral type");
					}
				}
				if(!componentsToConnect.contains(null)) {
					//Connect all the components
					Proposition andComponent = new Proposition(TEMP);

					andify(componentsToConnect, andComponent, trueComponent);
					if(!isThisConstant(andComponent, falseComponent)) {
						if(!inputsToOr.containsKey(sentence))
							inputsToOr.put(sentence, new HashSet<Component>());
						inputsToOr.get(sentence).add(andComponent);
						//We'll want to make sure at least one of the non-constant
						//components is changing
						if(preventDuplicatesFromConstants) {
							asnItr.changeOneInNext(varsInLiveConjuncts, assignment);
						}
					}
				}
			}
		}

		//At the end, we hook up the conjuncts
		for(Entry<GdlSentence, Set<Component>> entry : inputsToOr.entrySet()) {
			GdlSentence sentence = entry.getKey();
			Set<Component> inputs = entry.getValue();
			Set<Component> realInputs = new HashSet<Component>();
			for(Component input : inputs) {
				if(input instanceof Constant || input.getInputs().size() == 0) {
					realInputs.add(input);
				} else {
					realInputs.add(input.getSingleInput());
					input.getSingleInput().removeOutput(input);
					input.removeAllInputs();
				}
			}

			Proposition prop = new Proposition(sentence.toTerm());
			orify(realInputs, prop, falseComponent);
			components.put(sentence, prop);
		}

		//True/does sentences will have none of these rules, but
		//still need to exist/"float"
		//We'll do this if we haven't used base/input as a basis
		if(form.getName().equals(TRUE)
				|| form.getName().equals(DOES)) {
			for(GdlSentence sentence : form) {
				Proposition prop = new Proposition(sentence.toTerm());
				components.put(sentence, prop);
			}
		}

	}


	private static Set<GdlVariable> getVarsInLiveConjuncts(
			GdlRule rule, Set<SentenceForm> constantSentenceForms) {
		Set<GdlVariable> result = new HashSet<GdlVariable>();
		for(GdlLiteral literal : rule.getBody()) {
			if(literal instanceof GdlRelation) {
				if(!SentenceModel.inSentenceFormGroup((GdlRelation)literal, constantSentenceForms))
					result.addAll(SentenceModel.getVariables(literal));
			} else if(literal instanceof GdlNot) {
				GdlNot not = (GdlNot) literal;
				GdlSentence inner = (GdlSentence) not.getBody();
				if(!SentenceModel.inSentenceFormGroup(inner, constantSentenceForms))
					result.addAll(SentenceModel.getVariables(literal));
			}
		}
		return result;
	}

	private static boolean isThisConstant(Component conj, Constant constantComponent) {
		if(conj == constantComponent)
			return true;
		return (conj instanceof Proposition && conj.getInputs().size() == 1 && conj.getSingleInput() == constantComponent);
	}


	private static Not getNotOutput(Component positive) {
		for(Component c : positive.getOutputs()) {
			if(c instanceof Not) {
				return (Not) c;
			}
		}
		return null;
	}


	private static List<GdlVariable> getVarsInConjunct(GdlLiteral literal) {
		return SentenceModel.getVariables(literal);
	}


	private static void andify(List<Component> inputs, Component output, Constant trueProp) {
		//Special case: If the inputs include false, connect false to thisComponent
		for(Component c : inputs) {
			if(c instanceof Constant && !c.getValue()) {
				//Connect false (c) to the output
				output.addInput(c);
				c.addOutput(output);
				return;
			}
		}

		//For reals... just skip over any true constants
		And and = new And();
		for(Component in : inputs) {
			if(!(in instanceof Constant)) {
				in.addOutput(and);
				and.addInput(in);
			}
		}
		//What if they're all true? (Or inputs is empty?) Then no inputs at this point...
		if(and.getInputs().isEmpty()) {
			//Hook up to "true"
			trueProp.addOutput(output);
			output.addInput(trueProp);
			return;
		}
		//If there's just one, on the other hand, don't use the and gate
		if(and.getInputs().size() == 1) {
			Component in = and.getSingleInput();
			in.removeOutput(and);
			and.removeInput(in);
			in.addOutput(output);
			output.addInput(in);
			return;
		}
		and.addOutput(output);
		output.addInput(and);
	}

}
