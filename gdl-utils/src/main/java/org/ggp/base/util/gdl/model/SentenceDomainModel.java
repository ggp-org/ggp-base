package org.ggp.base.util.gdl.model;


/**
 * An extension of the SentenceFormModel that additionally
 * includes information about the domains of sentence forms.
 * In other words, this model specifies which constants can
 * be in which positions of each sentence form.
 *
 * The recommended way to create a SentenceDomainModel is
 * via {@link SentenceDomainModelFactory#createWithCartesianDomains(java.util.List)}.
 */
public interface SentenceDomainModel extends SentenceFormModel {
    /**
     * Gets the domain of a particular sentence form, which has
     * information about which particular sentences of the given
     * sentence form are possible.
     */
    SentenceFormDomain getDomain(SentenceForm form);
}
