package org.ggp.base.util.prover.aima.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;


public final class KnowledgeBase
{
	private final Map<GdlConstant, List<GdlRule>> contents;

	public KnowledgeBase(Set<? extends Gdl> description)
	{
		contents = new HashMap<GdlConstant, List<GdlRule>>();
		for (Gdl gdl : description)
		{
			GdlRule rule = (gdl instanceof GdlRule) ? (GdlRule) gdl : GdlPool.getRule((GdlSentence) gdl);
			GdlConstant key = rule.getHead().getName();

			if (!contents.containsKey(key))
			{
				contents.put(key, new ArrayList<GdlRule>());
			}
			contents.get(key).add(rule);
		}
	}

	public synchronized List<GdlRule> fetch(GdlSentence sentence)
	{
		GdlConstant key = sentence.getName();

		if (contents.containsKey(key))
		{
			return contents.get(key);
		}
		else
		{
			return new ArrayList<GdlRule>();
		}
	}
}