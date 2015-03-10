package org.ggp.base.util.prover.aima;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.transforms.DistinctAndNotMover;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.cache.ProverCache;
import org.ggp.base.util.prover.aima.knowledge.KnowledgeBase;
import org.ggp.base.util.prover.aima.renamer.VariableRenamer;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public final class AimaProver implements Prover
{

	private final KnowledgeBase knowledgeBase;

	private final ProverCache fixedAnswerCache = ProverCache.createMultiThreadedCache();

	public AimaProver(List<Gdl> description)
	{
		description = DistinctAndNotMover.run(description);
		knowledgeBase = new KnowledgeBase(Sets.newHashSet(description));
	}

	private Set<GdlSentence> ask(GdlSentence query, Set<GdlSentence> context, boolean askOne)
	{
		LinkedList<GdlLiteral> goals = new LinkedList<GdlLiteral>();
		goals.add(query);

		Set<Substitution> answers = new HashSet<Substitution>();
		ask(goals, new KnowledgeBase(context), new Substitution(), ProverCache.createSingleThreadedCache(),
				new VariableRenamer(), askOne, answers, new RecursionHandler(), new IsConstant());

		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution theta : answers)
		{
			results.add(Substituter.substitute(query, theta));
		}

		return results;
	}

	private void ask(LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, RecursionHandler recursionHandler, IsConstant isConstant)
	{
		if (goals.size() == 0)
		{
			results.add(theta);
			isConstant.value = true;
			return;
		}
		else
		{
			GdlLiteral literal = goals.removeFirst();
			GdlLiteral qPrime = Substituter.substitute(literal, theta);

			if (qPrime instanceof GdlDistinct)
			{
				GdlDistinct distinct = (GdlDistinct) qPrime;
				askDistinct(distinct, goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstant);
			}
			else if (qPrime instanceof GdlNot)
			{
				GdlNot not = (GdlNot) qPrime;
				askNot(not, goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstant);
			}
			else if (qPrime instanceof GdlOr)
			{
				GdlOr or = (GdlOr) qPrime;
				askOr(or, goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstant);
			}
			else
			{
				GdlSentence sentence = (GdlSentence) qPrime;
				askSentence(sentence, goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstant);
			}

			goals.addFirst(literal);
		}
	}

	@Override
	public Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context)
	{
		return ask(query, context, false);
	}

	private void askDistinct(GdlDistinct distinct, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, RecursionHandler recursionHandler, IsConstant isConstant)
	{
		if (!distinct.getArg1().equals(distinct.getArg2()))
		{
			ask(goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstant);
		} else {
			isConstant.value = true;
		}
	}

	private void askNot(GdlNot not, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, RecursionHandler recursionHandler, IsConstant isConstantRet)
	{
		LinkedList<GdlLiteral> notGoals = new LinkedList<GdlLiteral>();
		notGoals.add(not.getBody());

		Set<Substitution> notResults = new HashSet<Substitution>();
		boolean isConstant = true;
		ask(notGoals, context, theta, cache, renamer, true, notResults, recursionHandler, isConstantRet);
		isConstant &= isConstantRet.value;

		if (notResults.size() == 0)
		{
			ask(goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstantRet);
			isConstant &= isConstantRet.value;
		}
		isConstantRet.value = isConstant;
	}

	@Override
	public GdlSentence askOne(GdlSentence query, Set<GdlSentence> context)
	{
		Set<GdlSentence> results = ask(query, context, true);
		return (results.size() > 0) ? results.iterator().next() : null;
	}

	private void askOr(GdlOr or, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, RecursionHandler recursionHandler, IsConstant isConstantRet)
	{
		boolean isConstant = true;
		for (int i = 0; i < or.arity(); i++)
		{
			goals.addFirst(or.get(i));
			ask(goals, context, theta, cache, renamer, askOne, results, recursionHandler, isConstantRet);
			isConstant &= isConstantRet.value;
			goals.removeFirst();

			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
		isConstantRet.value = isConstant;
	}

	private void askSentence(GdlSentence sentence, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, RecursionHandler recursionHandler,
			IsConstant isConstantRet) {
		Collection<Substitution> sentenceResults = findSentenceResults(sentence,
				context, theta, cache, renamer, recursionHandler, isConstantRet);

		boolean isConstant = isConstantRet.value;
		for (Substitution thetaPrime : sentenceResults)
		{
			ask(goals, context, theta.compose(thetaPrime), cache, renamer, askOne, results, recursionHandler, isConstantRet);
			isConstant &= isConstantRet.value;
			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
		isConstantRet.value = isConstant;
	}

	private Collection<Substitution> findSentenceResults(GdlSentence sentence,
			KnowledgeBase context, Substitution theta,
			ProverCache cache, VariableRenamer renamer, RecursionHandler recursionHandler,
			IsConstant isConstantRet) {
		GdlSentence varRenamedSentence = new VariableRenamer().rename(sentence);
		if (!fixedAnswerCache.contains(varRenamedSentence) && !cache.contains(varRenamedSentence))
		{
			if (recursionHandler.alreadyAsking.contains(varRenamedSentence)) {
				//Mark that we're in recursive mode and shouldn't cache results
				recursionHandler.calledRecursively.add(varRenamedSentence);
				//Return stuff that we've seen as an answer for this before
				Collection<GdlSentence> previousResults = recursionHandler.previousResults.get(varRenamedSentence);
				List<Substitution> results = Lists.newArrayListWithCapacity(previousResults.size());
				for (GdlSentence knownResult : previousResults) {
					results.add(Unifier.unify(sentence, knownResult));
				}
				return results;
			}
			recursionHandler.alreadyAsking.add(varRenamedSentence);
			List<GdlRule> candidates = new ArrayList<GdlRule>();
			candidates.addAll(knowledgeBase.fetch(sentence));
			candidates.addAll(context.fetch(sentence));
			boolean isConstant = !isTrueOrDoesSentence(sentence);

			Set<Substitution> sentenceResults = new HashSet<Substitution>();
			for (GdlRule rule : candidates)
			{
				GdlRule r = renamer.rename(rule);
				Substitution thetaPrime = Unifier.unify(r.getHead(), sentence);

				if (thetaPrime != null)
				{
					LinkedList<GdlLiteral> sentenceGoals = new LinkedList<GdlLiteral>();
					for (int i = 0; i < r.arity(); i++)
					{
						sentenceGoals.add(r.get(i));
					}

					ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, recursionHandler, isConstantRet);
					isConstant &= isConstantRet.value;
				}
			}

			if (recursionHandler.calledRecursively.contains(varRenamedSentence)) {
				Set<GdlSentence> sentencesFromResults = Sets.newHashSet();
				for (Substitution result : sentenceResults) {
					sentencesFromResults.add(Substituter.substitute(sentence, result));
				}
				while (sentencesFromResults.size() > recursionHandler.previousResults.get(varRenamedSentence).size()) {
					recursionHandler.calledRecursively.remove(varRenamedSentence);
					recursionHandler.previousResults.putAll(varRenamedSentence, sentencesFromResults);

					sentenceResults = Sets.newHashSet();
					for (GdlRule rule : candidates) {
						GdlRule r = renamer.rename(rule);
						Substitution thetaPrime = Unifier.unify(r.getHead(), sentence);

						if (thetaPrime != null) {
							LinkedList<GdlLiteral> sentenceGoals = new LinkedList<GdlLiteral>();
							for (int i = 0; i < r.arity(); i++) {
								sentenceGoals.add(r.get(i));
							}

							ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, recursionHandler, isConstantRet);
							isConstant &= isConstantRet.value;
						}
					}
				}
				recursionHandler.calledRecursively.remove(varRenamedSentence);
			}

			recursionHandler.alreadyAsking.remove(varRenamedSentence);
			recursionHandler.previousResults.removeAll(varRenamedSentence);

			isConstantRet.value = isConstant;
			if (recursionHandler.calledRecursively.isEmpty()) {
				if (isConstant) {
					fixedAnswerCache.put(sentence, varRenamedSentence, sentenceResults);
				} else {
					cache.put(sentence, varRenamedSentence, sentenceResults);
				}
			}

			/*
			 * We filter results because they normally contain entries for all the variables
			 * encountered in subqueries, not just the sentence we're interested in. For some
			 * games (e.g. ruleDepthExponential) this is very expensive.
			 */
			return filterSentenceResults(sentence, sentenceResults);
		}

		List<Substitution> cachedResults = fixedAnswerCache.get(sentence, varRenamedSentence);
		isConstantRet.value = (cachedResults != null);
		if (cachedResults == null) {
			cachedResults = cache.get(sentence, varRenamedSentence);
		}
		return cachedResults;
	}

	private Collection<Substitution> filterSentenceResults(
			GdlSentence sentence, Set<Substitution> sentenceResults) {
		Set<GdlVariable> varsInSentence = GdlUtils.getVariablesSet(sentence);
		List<Substitution> results = Lists.newArrayListWithCapacity(sentenceResults.size());
		for (Substitution result : sentenceResults) {
			Substitution fixedResult = new Substitution();
			for (GdlVariable var : varsInSentence) {
				fixedResult.put(var, result.get(var));
			}
			results.add(fixedResult);
		}
		return results;
	}

	private boolean isTrueOrDoesSentence(GdlSentence sentence) {
		GdlConstant name = sentence.getName();
		return name == GdlPool.TRUE || name == GdlPool.DOES;
	}

	@Override
	public boolean prove(GdlSentence query, Set<GdlSentence> context)
	{
		return askOne(query, context) != null;
	}

	/*
	 * Mutable value holder; gets modified by methods it's passed to, as a kind of
	 * additional return value. Tracks whether queries involve "true" or "does" sentences;
	 * if not, their answers can be added to the fixedAnswerCache and reused across queries.
	 */
	private static class IsConstant {
		public boolean value = true;
	}

	/*
	 * Contains some mutable values used by the recursion implementation, to reduce
	 * the number of arguments being passed around.
	 *
	 * The general approach to handle recursion is to check for cases where we're
	 * querying a sentence we're already in the middle of querying. In that case, we
	 * return all the sentences we've found so far in the recursive query (initially
	 * the empty set), and we re-run the outer query until the number of sentences
	 * returned stops growing. (GDL restricts recursion in such a way that for a valid
	 * game description, no sentences will stop being true because we added a new sentence.)
	 * We also stop the caching of intermediate results while running a recursive query.
	 *
	 * This is not necessarily the most efficient approach, but it gives correct results.
	 */
	private static class RecursionHandler {
		public Set<GdlSentence> alreadyAsking = Sets.newHashSet();
		public Set<GdlSentence> calledRecursively = Sets.newHashSet();
		public Multimap<GdlSentence, GdlSentence> previousResults = HashMultimap.create();
	}
}
