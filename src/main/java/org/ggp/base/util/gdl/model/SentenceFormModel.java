package org.ggp.base.util.gdl.model;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.transforms.ConstantChecker;

import com.google.common.collect.Multimap;

/**
 * A model of the different types of sentences that may be true over
 * the course of a game.
 *
 * The SentenceFormModel uses the notion of a sentence form. This defines
 * the name of a sentence and the structure of functions within the
 * sentence.
 *
 * The recommended way of creating a SentenceFormModel is via
 * {@link SentenceFormModelFactory#create(List)}.
 */
public interface SentenceFormModel {
    /**
     * Returns the set of sentence forms that are independent; that is,
     * the truth values of the sentences of these forms may depend on
     * the turn of the game, but never on players' actions.
     *
     * For example, in tic-tac-toe, the sentence form (true (control _))
     * is independent, but not constant: it changes from turn to turn,
     * but always in the same way.
     *
     * All constant sentence forms are independent, so this is a superset
     * of {@link #getConstantSentenceForms()}.
     */
    Set<SentenceForm> getIndependentSentenceForms();

    /**
     * Returns the set of sentence forms that are constant; that is,
     * the truth values of sentences of these forms do not change
     * over the course of the game.
     *
     * The values of these sentences may be precomputed using a
     * {@link ConstantChecker}.
     */
    Set<SentenceForm> getConstantSentenceForms();

    /**
     * Returns a graph describing how the sentence forms relate to one
     * another in the rules of the game. One sentence form depends on
     * another if a rule producing the first sentence form has the
     * second sentence form in its body.
     *
     * Each key depends on the sentence forms in the associated collection.
     *
     * Note that this graph structure may contain cycles, and a sentence form
     * may depend on itself. Consider using
     * {@link DependencyGraphs#toposortSafe(Set, Multimap)} to obtain a
     * topological ordering in a way that respects cycles.
     */
    Multimap<SentenceForm, SentenceForm> getDependencyGraph();

    /**
     * Returns the list of sentences specifically listed as true in the
     * game description for that sentence form.
     */
    Set<GdlSentence> getSentencesListedAsTrue(SentenceForm form);

    /**
     * Returns the rules that GENERATE the sentence form, not necessarily
     * all the rules that contain it.
     *
     * Note that if functions can be assigned to variables, this might not
     * find all the rules capable of generating sentences of the given form.
     */
    Set<GdlRule> getRules(SentenceForm form);

    /**
     * Returns all sentence forms in the model.
     */
    Set<SentenceForm> getSentenceForms();

    /**
     * Returns the sentence form of the given sentence.
     */
    SentenceForm getSentenceForm(GdlSentence transformed);

    /**
     * Returns the game description for the game.
     */
    List<Gdl> getDescription();
}
