package org.ggp.base.util.gdl.model;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;

/**
 * A SentenceFormDomain contains information about the possible
 * sentences of a particular sentence form within a game. In other
 * words, it captures information about which constants can be
 * in which positions in the SentenceForm.
 */
public interface SentenceFormDomain extends Iterable<GdlSentence> {
    /**
     * Returns the SentenceForm associated with this domain.
     */
    SentenceForm getForm();

    /**
     * Returns a set containing every constant that can appear in
     * the given slot index in the sentence form.
     */
    Set<GdlConstant> getDomainForSlot(int slotIndex);
}
