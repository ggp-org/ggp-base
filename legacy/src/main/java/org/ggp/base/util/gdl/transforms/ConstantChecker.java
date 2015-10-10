package org.ggp.base.util.gdl.transforms;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormModel;

/**
 * A ConstantChecker provides information about which sentences are true
 * for the constant sentence forms in a game. These can be computed once
 * at the beginning of a match, to avoid redundant computations.
 *
 * The preferred way to create a ConstantChecker is with
 * {@link ConstantCheckerFactory#createWithForwardChaining(org.ggp.base.util.gdl.model.SentenceDomainModel)}.
 */
public interface ConstantChecker {
    /**
     * Returns true iff the sentence is of a constant form included in
     * this ConstantChecker.
     */
    boolean hasConstantForm(GdlSentence sentence);

    /**
     * Returns true iff the given sentence form is constant and is included
     * in this ConstantChecker.
     */
    boolean isConstantForm(SentenceForm form);

    /**
     * Returns the set of all true sentences of the given constant
     * sentence form.
     */
    Set<GdlSentence> getTrueSentences(SentenceForm form);

    /**
     * Returns the set of all constant sentence forms included
     * in this ConstantChecker.
     */
    Set<SentenceForm> getConstantSentenceForms();

    /**
     * Returns true iff the given sentence is of a constant
     * sentence form and is always true.
     */
    boolean isTrueConstant(GdlSentence sentence);

    /**
     * Returns the sentence form model that the constant checker is based on.
     */
    SentenceFormModel getSentenceFormModel();
}
