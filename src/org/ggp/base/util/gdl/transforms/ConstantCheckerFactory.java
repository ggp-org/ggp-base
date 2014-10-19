package org.ggp.base.util.gdl.transforms;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.DependencyGraphs;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormModel;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.reasoner.gdl.GdlChainingReasoner;
import org.ggp.base.util.reasoner.gdl.GdlSentenceSet;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class ConstantCheckerFactory {
	/**
	 * Precomputes the true sentences in every constant sentence form in the given
	 * sentence model and returns the results in the form of a ConstantChecker.
	 *
	 * The implementation uses a forward-chaining reasoner.
	 *
	 * For accurate results, the rules used should have had the {@link VariableConstrainer}
	 * transformation applied to them.
	 *
	 * On average, this approach is more efficient than {@link #createWithProver(SentenceFormModel)}.
	 */
	public static ImmutableConstantChecker createWithForwardChaining(SentenceDomainModel model) throws InterruptedException {
		GdlChainingReasoner reasoner = GdlChainingReasoner.create(model);
		GdlSentenceSet sentencesByForm = reasoner.getConstantSentences();
		addSentencesTrueByRulesDifferentially(sentencesByForm, model, reasoner);
		return ImmutableConstantChecker.create(model,
				Multimaps.filterKeys(sentencesByForm.getSentences(), Predicates.in(model.getConstantSentenceForms())));
	}

	private static void addSentencesTrueByRulesDifferentially(
			GdlSentenceSet sentencesByForm,
			SentenceDomainModel domainModel, GdlChainingReasoner reasoner) throws InterruptedException {
		SentenceFormModel model = domainModel;
		Set<SentenceForm> constantForms = model.getConstantSentenceForms();
		//Find the part of the dependency graph dealing only with the constant forms.
		Multimap<SentenceForm, SentenceForm> dependencySubgraph =
				Multimaps.filterKeys(model.getDependencyGraph(), Predicates.in(constantForms));
		dependencySubgraph = Multimaps.filterValues(model.getDependencyGraph(), Predicates.in(constantForms));
		dependencySubgraph = ImmutableMultimap.copyOf(dependencySubgraph);
		List<Set<SentenceForm>> ordering = DependencyGraphs.toposortSafe(constantForms, dependencySubgraph);

		for (Set<SentenceForm> stratum : ordering) {
			// One non-differential pass, collecting the changes
			GdlSentenceSet newlyTrueSentences = GdlSentenceSet.create();
			for (SentenceForm form : stratum) {
				for (GdlRule rule : model.getRules(form)) {
					GdlSentenceSet ruleResults =
							reasoner.getRuleResults(rule, domainModel, sentencesByForm);
					if (!reasoner.isSubsetOf(sentencesByForm, ruleResults)) {
						sentencesByForm = reasoner.getUnion(sentencesByForm, ruleResults);
						newlyTrueSentences = reasoner.getUnion(newlyTrueSentences, ruleResults);
					}
				}
			}

			// Now a lot of differential passes to deal with recursion efficiently
			boolean somethingChanged = true;
			while (somethingChanged) {
				somethingChanged = false;
				GdlSentenceSet newStuffInThisPass = GdlSentenceSet.create();
				for (SentenceForm form : stratum) {
					for (GdlRule rule : model.getRules(form)) {
						GdlSentenceSet ruleResults =
								reasoner.getRuleResultsForNewSentences(rule, domainModel, sentencesByForm,
										newlyTrueSentences);
						if (!reasoner.isSubsetOf(sentencesByForm, ruleResults)) {
							somethingChanged = true;
							newStuffInThisPass = reasoner.getUnion(newStuffInThisPass, ruleResults);
						}
					}
				}
				sentencesByForm = reasoner.getUnion(sentencesByForm, newStuffInThisPass);
				newlyTrueSentences = newStuffInThisPass;
			}
		}
	}

	/**
	 * Precomputes the true sentences in every constant sentence form in the given
	 * sentence model and returns the results in the form of a ConstantChecker.
	 *
	 * The implementation uses a backwards-chaining theorem prover.
	 *
	 * In most (but not all) cases, {@link #createWithForwardChaining(SentenceDomainModel)}
	 * is more efficient.
	 */
	public static ImmutableConstantChecker createWithProver(SentenceFormModel model) throws InterruptedException {
		Multimap<SentenceForm, GdlSentence> sentencesByForm = HashMultimap.create();
		addSentencesTrueByRules(sentencesByForm, model);
		return ImmutableConstantChecker.create(model, sentencesByForm);
	}

	private static void addSentencesTrueByRules(
			Multimap<SentenceForm, GdlSentence> sentencesByForm,
			SentenceFormModel model) throws InterruptedException {
		AimaProver prover = new AimaProver(model.getDescription());
		for (SentenceForm form : model.getConstantSentenceForms()) {
			GdlSentence query = form.getSentenceFromTuple(getVariablesTuple(form.getTupleSize()));
			for (GdlSentence result : prover.askAll(query, ImmutableSet.<GdlSentence>of())) {
				ConcurrencyUtils.checkForInterruption();
				//Variables may end up being replaced with functions, which is not
				//what we want here, so we have to double-check that the form is correct.
				if (form.matches(result)) {
					sentencesByForm.put(form, result);
				}
			}
		}
	}

	private static List<? extends GdlTerm> getVariablesTuple(int tupleSize) {
		List<GdlVariable> varsTuple = Lists.newArrayList();
		for (int i = 0; i < tupleSize; i++) {
			varsTuple.add(GdlPool.getVariable("?" + i));
		}
		return varsTuple;
	}
}
