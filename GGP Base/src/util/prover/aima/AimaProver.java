package util.prover.aima;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlOr;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.prover.Prover;
import util.prover.aima.cache.ProverCache;
import util.prover.aima.knowledge.KnowledgeBase;
import util.prover.aima.renamer.VariableRenamer;
import util.prover.aima.substituter.Substituter;
import util.prover.aima.substitution.Substitution;
import util.prover.aima.unifier.Unifier;

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
		ask(goals, new KnowledgeBase(context), new Substitution(), new ProverCache(), new VariableRenamer(), askOne, answers);

		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution theta : answers)
		{
			results.add(Substituter.substitute(query, theta));
		}

		return results;
	}

	private void ask(LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results)
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
				askDistinct(distinct, goals, context, theta, cache, renamer, askOne, results);
			}
			else if (qPrime instanceof GdlNot)
			{
				GdlNot not = (GdlNot) qPrime;
				askNot(not, goals, context, theta, cache, renamer, askOne, results);
			}
			else if (qPrime instanceof GdlOr)
			{
				GdlOr or = (GdlOr) qPrime;
				askOr(or, goals, context, theta, cache, renamer, askOne, results);
			}
			else
			{
				GdlSentence sentence = (GdlSentence) qPrime;
				askSentence(sentence, goals, context, theta, cache, renamer, askOne, results);
			}

			goals.addFirst(literal);
		}
	}

	@Override
	public Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context)
	{
		return ask(query, context, false);
	}

	private void askDistinct(GdlDistinct distinct, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results)
	{
		if (!distinct.getArg1().equals(distinct.getArg2()))
		{
			ask(goals, context, theta, cache, renamer, askOne, results);
		}
	}

	private void askNot(GdlNot not, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results)
	{
		LinkedList<GdlLiteral> notGoals = new LinkedList<GdlLiteral>();
		notGoals.add(not.getBody());

		Set<Substitution> notResults = new HashSet<Substitution>();
		ask(notGoals, context, theta, cache, renamer, true, notResults);

		if (notResults.size() == 0)
		{
			ask(goals, context, theta, cache, renamer, askOne, results);
		}
	}

	@Override
	public GdlSentence askOne(GdlSentence query, Set<GdlSentence> context)
	{
		Set<GdlSentence> results = ask(query, context, true);
		return (results.size() > 0) ? results.iterator().next() : null;
	}

	private void askOr(GdlOr or, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results)
	{
		for (int i = 0; i < or.arity(); i++)
		{
			goals.addFirst(or.get(i));
			ask(goals, context, theta, cache, renamer, askOne, results);
			goals.removeFirst();

			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
	}

	private void askSentence(GdlSentence sentence, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results)
	{
		if (!cache.contains(sentence))
		{
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

					ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults);
				}
			}

			cache.put(sentence, sentenceResults);
		}

		for (Substitution thetaPrime : cache.get(sentence))
		{
			ask(goals, context, theta.compose(thetaPrime), cache, renamer, askOne, results);
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
