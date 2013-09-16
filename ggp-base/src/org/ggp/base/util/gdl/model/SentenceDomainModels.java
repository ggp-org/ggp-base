package org.ggp.base.util.gdl.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class SentenceDomainModels {
	public static enum VarDomainOpts {
		INCLUDE_HEAD,
		BODY_ONLY
	}

	public static Map<GdlVariable, Set<GdlConstant>> getVarDomains(
			GdlRule rule,
			SentenceDomainModel domainModel,
			VarDomainOpts includeHead) {
		// For each positive definition of sentences in the rule, intersect their
		// domains everywhere the variables show up
		Multimap<GdlVariable, Set<GdlConstant>> varDomainsByVar = ArrayListMultimap.create();
		for (GdlLiteral literal : getSentences(rule, includeHead)) {
			if (literal instanceof GdlSentence) {
				GdlSentence sentence = (GdlSentence) literal;
				SentenceForm form = SimpleSentenceForm.create(sentence);
				SentenceFormDomain formWithDomain = domainModel.getDomain(form);

				List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
				for (int i = 0; i < tuple.size(); i++) {
					GdlTerm term = tuple.get(i);
					if (term instanceof GdlVariable) {
						GdlVariable var = (GdlVariable) term;
						Set<GdlConstant> domain = formWithDomain.getDomainForSlot(i);
						varDomainsByVar.put(var, domain);
					}
				}
			}
		}

		Map<GdlVariable, Set<GdlConstant>> varDomainByVar = combineDomains(varDomainsByVar);
		return varDomainByVar;
	}

	public static Iterable<GdlLiteral> getSentences(GdlRule rule, VarDomainOpts includeHead) {
		if (includeHead == VarDomainOpts.INCLUDE_HEAD) {
			return Iterables.concat(ImmutableList.of(rule.getHead()), rule.getBody());
		} else {
			return rule.getBody();
		}
	}

	private static Map<GdlVariable, Set<GdlConstant>> combineDomains(
			Multimap<GdlVariable, Set<GdlConstant>> varDomainsByVar) {
		return ImmutableMap.copyOf(Maps.transformValues(varDomainsByVar.asMap(),
				new Function<Collection<Set<GdlConstant>>, Set<GdlConstant>>() {
			@Override
			public Set<GdlConstant> apply(Collection<Set<GdlConstant>> input) {
				return intersectSets(input);
			}
		}));
	}

	private static <T> Set<T> intersectSets(
			Collection<Set<T>> input) {
		if (input.isEmpty()) {
			throw new IllegalArgumentException("Can't take an intersection of no sets");
		}
		Set<T> result = null;
		for (Set<T> set : input) {
			if (result == null) {
				result = Sets.newHashSet(set);
			} else {
				result.retainAll(set);
			}
		}
		assert result != null;
		return result;
	}
}
