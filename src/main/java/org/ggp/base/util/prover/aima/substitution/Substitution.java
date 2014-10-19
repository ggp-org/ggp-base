package org.ggp.base.util.prover.aima.substitution;

import java.util.HashMap;
import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;


public final class Substitution
{

	private final Map<GdlVariable, GdlTerm> contents;

	public Substitution()
	{
		contents = new HashMap<GdlVariable, GdlTerm>();
	}

	public Substitution compose(Substitution thetaPrime)
	{
		Substitution result = new Substitution();

		result.contents.putAll(contents);
		result.contents.putAll(thetaPrime.contents);

		return result;
	}

	public boolean contains(GdlVariable variable)
	{
		return contents.containsKey(variable);
	}

	@Override
	public boolean equals(Object o)
	{
		if ((o != null) && (o instanceof Substitution))
		{
			Substitution substitution = (Substitution) o;
			return substitution.contents.equals(contents);
		}

		return false;
	}

	public GdlTerm get(GdlVariable variable)
	{
		return contents.get(variable);
	}

	@Override
	public int hashCode()
	{
		return contents.hashCode();
	}

	public void put(GdlVariable variable, GdlTerm term)
	{
		contents.put(variable, term);
	}

	/**
	 * Creates an identical substitution.
	 *
	 * @return A new, identical substitution.
	 */
	public Substitution copy()
	{
		Substitution copy = new Substitution();
		copy.contents.putAll(contents);
		return copy;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("{ ");
		for (GdlVariable variable : contents.keySet())
		{
			sb.append(variable + "/" + contents.get(variable) + " ");
		}
		sb.append("}");

		return sb.toString();
	}

}
