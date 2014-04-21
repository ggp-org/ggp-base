package org.ggp.base.util.prover.aima.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;


public final class ProverCache
{

	private final Map<GdlSentence, Set<GdlSentence>> contents;

	private ProverCache(Map<GdlSentence, Set<GdlSentence>> mapForContents) {
		this.contents = mapForContents;
	}

	public static ProverCache createSingleThreadedCache() {
		return new ProverCache(new HashMap<GdlSentence, Set<GdlSentence>>());
	}

	public static ProverCache createMultiThreadedCache() {
		return new ProverCache(new ConcurrentHashMap<GdlSentence, Set<GdlSentence>>());
	}

	/**
	 * NOTE: The given sentence must have been renamed with a VariableRenamer.
	 */
	public boolean contains(GdlSentence renamedSentence)
	{
		return contents.containsKey(renamedSentence);
	}

	public List<Substitution> get(GdlSentence sentence, GdlSentence varRenamedSentence)
	{
		Set<GdlSentence> cacheContents = contents.get(varRenamedSentence);
		if (cacheContents == null) {
			return null;
		}
		Set<Substitution> results = new HashSet<Substitution>();
		for (GdlSentence answer : cacheContents)
		{
			results.add(Unifier.unify(sentence, answer));
		}

		return new ArrayList<Substitution>(results);
	}

	public void put(GdlSentence sentence, GdlSentence renamedSentence,
			Set<Substitution> answers)
	{
		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution answer : answers)
		{
			results.add(Substituter.substitute(sentence, answer));
		}

		contents.put(renamedSentence, results);
	}

}
