package org.ggp.base.util.prover.aima;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.transforms.DistinctAndNotMover;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.cache.ProverCache;
import org.ggp.base.util.prover.aima.knowledge.KnowledgeBase;
import org.ggp.base.util.prover.aima.renamer.VariableRenamer;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;

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
		Set<GdlSentence> alreadyAsking = new HashSet<GdlSentence>();
		ask(goals, new KnowledgeBase(context), new Substitution(), ProverCache.createSingleThreadedCache(),
				new VariableRenamer(), askOne, answers, alreadyAsking);

		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution theta : answers)
		{
			results.add(Substituter.substitute(query, theta));
		}

		return results;
	}

	// Returns true iff the result is constant across all possible states of the game.
	private boolean ask(LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (goals.size() == 0)
		{
			results.add(theta);
			return true;
		}
		else
		{
			GdlLiteral literal = goals.removeFirst();
			GdlLiteral qPrime = Substituter.substitute(literal, theta);

			boolean isConstant;

			if (qPrime instanceof GdlDistinct)
			{
				GdlDistinct distinct = (GdlDistinct) qPrime;
				isConstant = askDistinct(distinct, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else if (qPrime instanceof GdlNot)
			{
				GdlNot not = (GdlNot) qPrime;
				isConstant = askNot(not, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else if (qPrime instanceof GdlOr)
			{
				GdlOr or = (GdlOr) qPrime;
				isConstant = askOr(or, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else
			{
				GdlSentence sentence = (GdlSentence) qPrime;
				isConstant = askSentence(sentence, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}

			goals.addFirst(literal);
			return isConstant;
		}
	}

	@Override
	public Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context)
	{
		return ask(query, context, false);
	}

	// Returns true iff the result is constant across all possible states of the game.
	private boolean askDistinct(GdlDistinct distinct, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (!distinct.getArg1().equals(distinct.getArg2()))
		{
			return ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
		return true;
	}

	// Returns true iff the result is constant across all possible states of the game.
	private boolean askNot(GdlNot not, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		LinkedList<GdlLiteral> notGoals = new LinkedList<GdlLiteral>();
		notGoals.add(not.getBody());

		Set<Substitution> notResults = new HashSet<Substitution>();
		boolean isConstant = true;
		isConstant &= ask(notGoals, context, theta, cache, renamer, true, notResults, alreadyAsking);

		if (notResults.size() == 0)
		{
			isConstant &= ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
		return isConstant;
	}

	@Override
	public GdlSentence askOne(GdlSentence query, Set<GdlSentence> context)
	{
		Set<GdlSentence> results = ask(query, context, true);
		return (results.size() > 0) ? results.iterator().next() : null;
	}

	// Returns true iff the result is constant across all possible states of the game.
	private boolean askOr(GdlOr or, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		boolean isConstant = true;
		for (int i = 0; i < or.arity(); i++)
		{
			goals.addFirst(or.get(i));
			isConstant &= ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			goals.removeFirst();

			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
		return isConstant;
	}

	// Returns true iff the result is constant across all possible states of the game.
	private boolean askSentence(GdlSentence sentence, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		GdlSentence varRenamedSentence = new VariableRenamer().rename(sentence);
		if (!fixedAnswerCache.contains(varRenamedSentence) && !cache.contains(varRenamedSentence))
		{
			//Prevent infinite loops on certain recursive queries.
			if(alreadyAsking.contains(sentence)) {
				return false;
			}
			alreadyAsking.add(sentence);
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

					isConstant &= ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, alreadyAsking);
				}
			}

			if (isConstant) {
				fixedAnswerCache.put(sentence, varRenamedSentence, sentenceResults);
			} else {
				cache.put(sentence, varRenamedSentence, sentenceResults);
			}

			alreadyAsking.remove(sentence);
		}

		List<Substitution> cachedResults = fixedAnswerCache.get(sentence, varRenamedSentence);
		boolean isConstant = (cachedResults != null);
		if (cachedResults == null) {
			cachedResults = cache.get(sentence, varRenamedSentence);
		}
		for (Substitution thetaPrime : cachedResults)
		{
			isConstant &= ask(goals, context, theta.compose(thetaPrime), cache, renamer, askOne, results, alreadyAsking);
			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
		return isConstant;
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

}
