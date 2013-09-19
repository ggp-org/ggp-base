package org.ggp.base.util.reasoner;

import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.transforms.VariableConstrainer;

/**
 * An extension of the ForwardChainingReasoner that allows for finding
 * just the consequences of a rule that result from a particular input.
 * This can be a much more efficient way of handling recursive rules.
 */
public interface DifferentialForwardChainingReasoner<Rule, Sentences>
						extends ForwardChainingReasoner<Rule, Sentences> {
	/**
	 * Given a rule, all sentences known to be true, and a set of new sentences,
	 * returns all new results of the rule that involve at least one of the new
	 * sentences as a positive literal in the body of the rule. Sentences already
	 * known to be true will not be included in the result.
	 *
	 * This can be a more efficient way of dealing with recursive rules than
	 * {@link #getRuleResults(Object, SentenceDomainModel, Object)}.
	 *
	 * For the outputs of this method to be valid, the GDL that the rule
	 * is derived from should have had the {@link VariableConstrainer}
	 * transformation applied to it.
	 */
	Sentences getRuleResultsForNewSentences(Rule rule, SentenceDomainModel domainModel,
			Sentences allSentences, Sentences newSentences) throws InterruptedException;
}
