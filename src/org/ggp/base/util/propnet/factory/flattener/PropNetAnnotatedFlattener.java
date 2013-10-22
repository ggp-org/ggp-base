package org.ggp.base.util.propnet.factory.flattener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.transforms.DeORer;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.factory.annotater.PropNetAnnotater;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;


/**
 * PropNetAnnotatedFlattener is an implementation of a GDL flattener that needs
 * the rules to contain ( base ?x ) propositions that explicitly specify domains
 * for all of the base propositions.
 * 
 * This flattener should work on all sizes of games, but requires these explicit
 * annotations to be present in the game description; it cannot infer them.
 * 
 * A separate PropNetAnnotater class is able to generate these annotations.
 * Sadly, it can only annotate relatively simple games. If there are no base
 * propositions in a game description, PropNetAnnotatedFlattener will call
 * the annotater in an attempt to generate annotations.
 * 
 * To use this class:
 *      PropNetAnnotatedFlattener AF = new PropNetAnnotatedFlattener(description);
 *      List<GdlRule> flatDescription = AF.flatten();
 *      return converter.convert(flatDescription);
 */
public final class PropNetAnnotatedFlattener
{
    /** An archive of Rule instantiations, indexed by head name. */
    private Map<GdlConstant, List<GdlRule>> instantiations;
    /** An archive of the rules in a game description, indexed by head name. */
    private Map<GdlConstant, List<GdlRule>> templates;
    
    /**
     * Construct a BasicPropNetFlattener for a given game.
     */
    private List<Gdl> description;
    public PropNetAnnotatedFlattener(List<Gdl> description) {
        this.description = description;
    }

    /**
     * Flattens a game description using the following process:
     * <ol>
     * <li>Records the rules in the description, and indexes them by head name.</li>
     * <li>Creates an archive of rule instantiations, initialized with
     * <tt>true</tt> rules.</li>
     * <li>Creates every instantiation of each rule in the description and
     * records the result.</li>
     * </ol>
     * 
     * @param description
     *            A game description.
     * @return An equivalent description, without variables.
     */
    public List<GdlRule> flatten()
    {        
        description = DeORer.run(description);
        if (noAnnotations()) {
            GamerLogger.log("StateMachine", "Could not find 'base' annotations. Attempting to generate them...");
            description = new PropNetAnnotater(description).getAugmentedDescription();
            GamerLogger.log("StateMachine", "Annotations generated.");
        }
        
        templates = recordTemplates(description);
        instantiations = initializeInstantiations(description);

        List<GdlRule> flatDescription = new ArrayList<GdlRule>();
        for ( GdlConstant constant : templates.keySet() )
        {
            flatDescription.addAll(getInstantiations(constant));
        }

        return flatDescription;
    }
    
    public boolean noAnnotations() {
        for ( Gdl gdl : description ) {
            if ( ! (gdl instanceof GdlSentence) ) continue;            
            GdlSentence sentence = (GdlSentence) gdl;
            
            if (sentence.getName().getValue().equals("base"))
                return false;
        }
        
        return true;
    }

    /**
     * Recursive method that uses a <tt>base</tt> rule to build a set of
     * <tt>true</tt> instantiations. Given a <tt>base</tt> rule of the form
     * <tt>(base name (arg1 a11 a12 ... a1n) ... (argn an1 an2 ... ann))</tt>
     * the method will return every possible combination of rules with name
     * <tt>name</tt> and args <tt>arg1</tt> through <tt>argn</tt>.
     * 
     * @param base
     *            A <tt>base</tt> rule.
     * @param index
     *            The index of the rule being considered.
     * @param workingSet
     *            The list of GdlTerms built up so far.
     * @param results
     *            The list of results built up so far.
     */
    private void expandTrue(GdlSentence base, int index, LinkedList<GdlTerm> workingSet, List<GdlRule> results)
    {
        if ( base.arity() == index )
        {
            GdlConstant name = (GdlConstant) base.get(0);
            List<GdlTerm> body = new ArrayList<GdlTerm>(workingSet);

            GdlFunction function = GdlPool.getFunction(name, body);
            results.add(GdlPool.getRule(GdlPool.getRelation(GdlPool.getConstant("true"), new GdlTerm[] { function })));
        }
        else
        {
            for ( GdlTerm term : ((GdlFunction) base.get(index)).getBody() )
            {
                workingSet.addLast(term);
                expandTrue(base, index + 1, workingSet, results);
                workingSet.removeLast();
            }
        }
    }

    /**
     * Returns the list of instantiations associated with the head name
     * <tt>constant</tt>, building one first if necessary. Rules with heads
     * named <tt>legal</tt> are generated by looking up the instantiation for
     * rules with heads named <tt>does</tt> and renaming the results.
     * 
     * @param constant
     *            The name of the head of the rule to instantiate.
     * @return A list of instantiations.
     */
    private List<GdlRule> getInstantiations(GdlConstant constant)
    {
        if ( !instantiations.containsKey(constant) )
        {
            instantiations.put(constant, new ArrayList<GdlRule>());

            if ( constant.getValue().equals("does") )
            {
                for ( GdlRule rule : getInstantiations(GdlPool.getConstant("legal")) )
                {
                    GdlSentence head = GdlPool.getRelation(GdlPool.getConstant("does"), rule.getHead().getBody());
                    GdlRule equivalentDoesRule = GdlPool.getRule(head);
                    instantiations.get(constant).add(equivalentDoesRule);
                }
            }
            else
            {
                for ( GdlRule template : templates.get(constant) )
                {
                    Set<GdlRule> results = new HashSet<GdlRule>();
                    instantiate(template, 0, new Substitution(), results);

                    instantiations.get(constant).addAll(results);
                }
            }
        }

        return instantiations.get(constant);
    }

    /**
     * Creates an archive of rule instantiations from a game description,
     * initialized with <tt>true</tt> rules, built using the reserved
     * <tt>base</tt> keyword.
     * 
     * @param description
     *            A game description.
     * @return An archive of rule instantiations, indexed by head name.
     */
    private Map<GdlConstant, List<GdlRule>> initializeInstantiations(List<Gdl> description)
    {
        List<GdlRule> trues = new ArrayList<GdlRule>();
        for ( Gdl gdl : description )
        {
            if ( gdl instanceof GdlSentence )
            {
                GdlSentence sentence = (GdlSentence) gdl;
                if ( sentence.getName().getValue().equals("base") )
                {
                    if ( sentence.arity() == 1 )
                    {
                        GdlConstant constant = (GdlConstant) sentence.get(0);
                        trues.add(GdlPool.getRule(GdlPool.getRelation(GdlPool.getConstant("true"), new GdlTerm[] { constant })));
                    }
                    else
                    {
                        List<GdlRule> results = new ArrayList<GdlRule>();
                        expandTrue(sentence, 1, new LinkedList<GdlTerm>(), results);

                        trues.addAll(results);
                    }
                }
            }
        }

        Map<GdlConstant, List<GdlRule>> instantiations = new HashMap<GdlConstant, List<GdlRule>>();
        instantiations.put(GdlPool.getConstant("true"), trues);

        return instantiations;
    }

    /**
     * A recursive method for generating every possible instantiation of a rule.
     * Instantiations are generated by created every possible combination of
     * instantiations to the literals in the body of the rule so long as those
     * instantiations do not conflict with each other.
     * 
     * @param template
     *            The rule to instantiate.
     * @param index
     *            The literal in the body currently being considered.
     * @param theta
     *            The substitution built up so far.
     * @param results
     *            The list of results built up so far.
     */
    private void instantiate(GdlRule template, int index, Substitution theta, Set<GdlRule> results)
    {
        if ( template.getBody().size() == index )
        {
            results.add(Substituter.substitute(template, theta));
        }
        else
        {
            GdlLiteral literal = template.getBody().get(index);
            if ( literal instanceof GdlSentence )
            {
                GdlSentence sentence = (GdlSentence) Substituter.substitute(literal, theta);
                for ( GdlRule instantiation : getInstantiations(sentence.getName()) )
                {
                    Substitution thetaPrime = Unifier.unify(sentence, instantiation.getHead());
                    if ( thetaPrime!=null )
                    {
                        Substitution thetaCopy = theta.copy();
                        thetaCopy = thetaCopy.compose(thetaPrime);

                        instantiate(template, index + 1, thetaCopy, results);
                    }
                }
            }
            else
            {
                instantiate(template, index + 1, theta, results);
            }
        }
    }

    /**
     * Records the rules in a game description, indexed by head name. Ignores
     * rules that begin with the reserved <tt>base</tt> keyword.
     * 
     * @param description
     *            A game description.
     * @return An archive of rules, indexed by head name.
     */
    private Map<GdlConstant, List<GdlRule>> recordTemplates(List<Gdl> description)
    {
        Map<GdlConstant, List<GdlRule>> templates = new HashMap<GdlConstant, List<GdlRule>>();
        for ( Gdl gdl : description )
        {
            GdlRule rule = (gdl instanceof GdlRule) ? (GdlRule) gdl : GdlPool.getRule((GdlSentence) gdl);
            GdlConstant name = rule.getHead().getName();

            if ( !name.getValue().equals("base") )
            {
                if ( !templates.containsKey(name) )
                {
                    templates.put(name, new ArrayList<GdlRule>());
                }
                templates.get(name).add(rule);
            }
        }

        return templates;
    }
}