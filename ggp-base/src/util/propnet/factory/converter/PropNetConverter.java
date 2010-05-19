package util.propnet.factory.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import util.statemachine.Role;

/**
 * The PropNetConverter class defines PropNet conversion for the PropNetFactory
 * class. This takes in a flattened game description, and converts it into an
 * equivalent PropNet.
 */
public final class PropNetConverter
{
	/** An archive of Propositions, indexed by name. */
	private Map<GdlTerm, Proposition> propositions;
	/** An archive of Components. */
	private Set<Component> components;

	/**
	 * Converts a game description to a PropNet using the following process
	 * (note that this method and all of the methods that it invokes assume that
	 * <tt>description</tt> has already been flattened by a PropNetFlattener):
	 * <ol>
	 * <li>Transforms each of the rules in <tt>description</tt> into
	 * equivalent PropNet Components.</li>
	 * <li>Adds or gates to Propositions with more than one input.</li>
	 * <li>Adds inputs that are implicitly specified by <tt>description</tt>.</li>
	 * </ol>
	 * 
	 * @param description
	 *            A game description.
	 * @return An equivalent PropNet.
	 */
	public PropNet convert(List<GdlRule> description)
	{
		propositions = new HashMap<GdlTerm, Proposition>();
		components = new HashSet<Component>();

		for ( GdlRule rule : description )
		{
			if ( rule.arity() > 0 )
			{
				convertRule(rule);
			}
			else
			{
				convertStatic(rule.getHead());
			}
		}

		fixDisjunctions();
		addMissingInputs();
		
		return new PropNet(Role.computeRoles(description), components);
	}

	/**
	 * Creates an equivalent InputProposition for every LegalProposition where
	 * none already exists.
	 */
	private void addMissingInputs()
	{
		List<Proposition> addList = new ArrayList<Proposition>();
		for ( Proposition proposition : propositions.values() )
		{
			if ( proposition.getName() instanceof GdlFunction )
			{
				GdlFunction function = (GdlFunction) proposition.getName();
				if ( function.getName().getValue().equals("legal") )
				{
					addList.add(proposition);
				}
			}
		}

		for ( Proposition addItem : addList )
		{
			GdlFunction function = (GdlFunction) addItem.getName();
			components.add(getProposition(GdlPool.getFunction(GdlPool.getConstant("does"), function.getBody())));
		}
	}

	/**
	 * Converts a literal to equivalent PropNet Components and returns a
	 * reference to the last of those components.
	 * 
	 * @param literal
	 *            The literal to convert to equivalent PropNet Components.
	 * @return The last of those components.
	 */
	private Proposition convertConjunct(GdlLiteral literal)
	{
		if ( literal instanceof GdlDistinct )
		{
			GdlDistinct distinct = (GdlDistinct) literal;

			Proposition proposition = new Proposition(GdlPool.getConstant("anon"));
			Constant constant = new Constant(!distinct.getArg1().equals(distinct.getArg2()));

			link(constant, proposition);

			components.add(proposition);
			components.add(constant);

			return proposition;
		}
		else if ( literal instanceof GdlNot )
		{
			GdlNot not = (GdlNot) literal;

			Proposition input = convertConjunct(not.getBody());
			Not no = new Not();
			Proposition output = new Proposition(GdlPool.getConstant("anon"));

			link(input, no);
			link(no, output);

			components.add(input);
			components.add(no);
			components.add(output);

			return output;
		}
		else
		{
			GdlSentence sentence = (GdlSentence) literal;

			Proposition proposition = (sentence.getName().getValue().equals("true")) ? getProposition(sentence.get(0)) : getProposition(sentence.toTerm());
			components.add(proposition);

			return proposition;
		}
	}

	/**
	 * Converts a sentence to equivalent PropNet Components and returns the
	 * first of those components.
	 * 
	 * @param sentence
	 *            The sentence to convert to equivalent PropNet Components.
	 * @return The first of those Components.
	 */
	private Proposition convertHead(GdlSentence sentence)
	{
		if ( sentence.getName().getValue().equals("next") )
		{
			Proposition head = getProposition(sentence.get(0));
			Transition transition = new Transition();
			Proposition preTransition = new Proposition(GdlPool.getConstant("anon"));

			link(preTransition, transition);
			link(transition, head);

			components.add(head);
			components.add(transition);
			components.add(preTransition);

			return preTransition;
		}
		else
		{
			Proposition proposition = getProposition(sentence.toTerm());
			components.add(proposition);

			return proposition;
		}
	}

	/**
	 * Converts a rule into equivalent PropNet Components by invoking the
	 * <tt>convertHead()</tt> method on the head, and the
	 * <tt>convertConjunct</tt> method on every literal in the body and
	 * joining the results by an and gate.
	 * 
	 * @param rule
	 *            The rule to convert.
	 */
	private void convertRule(GdlRule rule)
	{
		Proposition head = convertHead(rule.getHead());
		And and = new And();

		link(and, head);

		components.add(head);
		components.add(and);

		for ( GdlLiteral literal : rule.getBody() )
		{
			Proposition conjunct = convertConjunct(literal);
			link(conjunct, and);
		}
	}

	/**
	 * Converts a sentence to equivalent PropNet Components.
	 * 
	 * @param sentence
	 *            The sentence to convert to equivalent PropNet Components.
	 */
	private void convertStatic(GdlSentence sentence)
	{
		if ( sentence.getName().getValue().equals("init") )
		{
			Proposition init = getProposition(GdlPool.getConstant("INIT"));
			Transition transition = new Transition();
			Proposition proposition = getProposition(sentence.get(0));

			link(init, transition);
			link(transition, proposition);

			components.add(init);
			components.add(transition);
			components.add(proposition);
		}

		Constant constant = new Constant(true);
		Proposition proposition = getProposition(sentence.toTerm());

		link(constant, proposition);

		components.add(constant);
		components.add(proposition);
	}

	/**
	 * Creates an or gate to combine the inputs to a Proposition wherever one
	 * has more than one input.
	 */
	private void fixDisjunctions()
	{
		List<Proposition> fixList = new ArrayList<Proposition>();
		for ( Proposition proposition : propositions.values() )
		{
			if ( proposition.getInputs().size() > 1 )
			{
				fixList.add(proposition);
			}
		}

		for ( Proposition fixItem : fixList )
		{
			Or or = new Or();
			int i = 0;
			for ( Component input : fixItem.getInputs() )
			{
			    i++;

				Proposition disjunct = null;
				if ( fixItem.getName() instanceof GdlConstant )
				{
					GdlConstant constant = (GdlConstant) fixItem.getName();
					disjunct = new Proposition(GdlPool.getConstant(constant.getValue() + "-" + i));
				}
				else
				{
					GdlFunction function = (GdlFunction) fixItem.getName();
					disjunct = new Proposition(GdlPool.getFunction(GdlPool.getConstant(function.getName().getValue() + "-" + i), function.getBody()));
				}

				input.getOutputs().clear();

				link(input, disjunct);
				link(disjunct, or);

				components.add(disjunct);
			}

			fixItem.getInputs().clear();
			link(or, fixItem);

			components.add(or);
		}
	}

	/**
	 * Returns a Proposition with name <tt>term</tt>, creating one if none
	 * already exists.
	 * 
	 * @param term
	 *            The name of the Proposition.
	 * @return A Proposition with name <tt>term</tt>.
	 */
	private Proposition getProposition(GdlTerm term)
	{
		if ( !propositions.containsKey(term) )
		{
			propositions.put(term, new Proposition(term));
		}
		return propositions.get(term);
	}

	/**
	 * Adds inputs and outputs to <tt>source</tt> and <tt>target</tt> such
	 * that <tt>source becomes an input to <tt>target</tt>.
	 * @param source A component.
	 * @param target A second component.
	 */
	private void link(Component source, Component target)
	{
		source.addOutput(target);
		target.addInput(source);
	}
}