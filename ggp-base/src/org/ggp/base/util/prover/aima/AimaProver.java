package org.ggp.base.util.prover.aima;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.cache.ProverCache;
import org.ggp.base.util.prover.aima.knowledge.KnowledgeBase;
import org.ggp.base.util.prover.aima.renamer.VariableRenamer;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;


public final class AimaProver extends Prover
{

	private final KnowledgeBase knowledgeBase;

	public AimaProver(Set<Gdl> description)
	{
		knowledgeBase = new KnowledgeBase(description);
	}

	private Set<GdlSentence> ask(GdlSentence query, Set<GdlSentence> context, boolean askOne)
	{
		LinkedList<GdlLiteral> goals = new LinkedList<GdlLiteral>();
		goals.add(query);

		Set<Substitution> answers = new HashSet<Substitution>();
		Set<GdlSentence> alreadyAsking = new HashSet<GdlSentence>();
		ask(goals, new KnowledgeBase(context), new Substitution(), new ProverCache(), new VariableRenamer(), askOne, answers, alreadyAsking);

		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution theta : answers)
		{
			results.add(Substituter.substitute(query, theta));
		}

		return results;
	}

	private void ask(LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (goals.size() == 0)
		{
			results.add(theta);
		}
		else
		{
			GdlLiteral literal = goals.removeFirst();
			GdlLiteral qPrime = Substituter.substitute(literal, theta);

			if (qPrime instanceof GdlDistinct)
			{
				GdlDistinct distinct = (GdlDistinct) qPrime;
				askDistinct(distinct, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else if (qPrime instanceof GdlNot)
			{
				GdlNot not = (GdlNot) qPrime;
				askNot(not, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else if (qPrime instanceof GdlOr)
			{
				GdlOr or = (GdlOr) qPrime;
				askOr(or, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else
			{
				GdlSentence sentence = (GdlSentence) qPrime;
				askSentence(sentence, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}

			goals.addFirst(literal);
		}
	}

	@Override
	public Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context)
	{
		return ask(query, context, false);
	}

	private void askDistinct(GdlDistinct distinct, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (!distinct.getArg1().equals(distinct.getArg2()))
		{
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
	}

	private void askNot(GdlNot not, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		LinkedList<GdlLiteral> notGoals = new LinkedList<GdlLiteral>();
		notGoals.add(not.getBody());

		Set<Substitution> notResults = new HashSet<Substitution>();
		ask(notGoals, context, theta, cache, renamer, true, notResults, alreadyAsking);

		if (notResults.size() == 0)
		{
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
	}

	@Override
	public GdlSentence askOne(GdlSentence query, Set<GdlSentence> context)
	{
		Set<GdlSentence> results = ask(query, context, true);
		return (results.size() > 0) ? results.iterator().next() : null;
	}

	private void askOr(GdlOr or, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		for (int i = 0; i < or.arity(); i++)
		{
			goals.addFirst(or.get(i));
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			goals.removeFirst();

			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
	}

	private void askSentence(GdlSentence sentence, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (!cache.contains(sentence))
		{
			//Prevent infinite loops on certain recursive queries.
			if(alreadyAsking.contains(sentence)) {
				return;
			}
			alreadyAsking.add(sentence);
			List<GdlRule> candidates = new ArrayList<GdlRule>();
			candidates.addAll(knowledgeBase.fetch(sentence));
			candidates.addAll(context.fetch(sentence));

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

					ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, alreadyAsking);
				}
			}

			cache.put(sentence, sentenceResults);
			alreadyAsking.remove(sentence);
		}

		for (Substitution thetaPrime : cache.get(sentence))
		{
			ask(goals, context, theta.compose(thetaPrime), cache, renamer, askOne, results, alreadyAsking);
			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
	}

	@Override
	public boolean prove(GdlSentence query, Set<GdlSentence> context)
	{
		return askOne(query, context) != null;
	}

}
