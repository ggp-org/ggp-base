package org.ggp.base.util.prover.aima.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.prover.aima.renamer.VariableRenamer;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;


public final class ProverCache
{

	private final Map<GdlSentence, Set<GdlSentence>> contents;

	public ProverCache()
	{
		contents = new HashMap<GdlSentence, Set<GdlSentence>>();
	}

	public boolean contains(GdlSentence sentence)
	{
		return contents.containsKey(new VariableRenamer().rename(sentence));
	}

	public List<Substitution> get(GdlSentence sentence)
	{
		Set<Substitution> results = new HashSet<Substitution>();
		for (GdlSentence answer : contents.get(new VariableRenamer().rename(sentence)))
		{
			results.add(Unifier.unify(sentence, answer));
		}

		return new ArrayList<Substitution>(results);
	}

	public void put(GdlSentence sentence, Set<Substitution> answers)
	{
		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution answer : answers)
		{
			results.add(Substituter.substitute(sentence, answer));
		}

		contents.put(new VariableRenamer().rename(sentence), results);
	}

}
