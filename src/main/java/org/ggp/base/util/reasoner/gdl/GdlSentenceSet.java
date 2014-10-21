package org.ggp.base.util.reasoner.gdl;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.assignments.AddibleFunctionInfo;
import org.ggp.base.util.gdl.model.assignments.MutableFunctionInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * Contains a set of GdlSentences arranged by SentenceForm and the
 * associated FunctionInfo for each SentenceForm. The FunctionInfos
 * are continually and automatically maintained as sentences are
 * added to the set.
 *
 * Note that this class is not thread-safe.
 */
public class GdlSentenceSet {
	private final SetMultimap<SentenceForm, GdlSentence> sentences;
	private final Map<SentenceForm, AddibleFunctionInfo> functionInfoMap;

	private GdlSentenceSet() {
		this.sentences = HashMultimap.create();
		this.functionInfoMap = Maps.newHashMap();
	}

	public static GdlSentenceSet create() {
		return new GdlSentenceSet();
	}

	public static GdlSentenceSet create(Multimap<SentenceForm, GdlSentence> sentences) {
		GdlSentenceSet result = create();
		result.putAll(sentences);
		return result;
	}

	/**
	 * Returns an unmodifiable view of the sentences in this set.
	 * Note that this view may change if the collection is concurrently
	 * modified.
	 */
	public SetMultimap<SentenceForm, GdlSentence> getSentences() {
		return Multimaps.unmodifiableSetMultimap(sentences);
	}

	/**
	 * Returns true iff the given sentence is in this set of sentences.
	 */
	public boolean containsSentence(SentenceForm form, GdlSentence sentence) {
		return sentences.containsEntry(form, sentence);
	}

	public void putAll(Multimap<SentenceForm, GdlSentence> newSentences) {
		for (Entry<SentenceForm, GdlSentence> entry : newSentences.entries()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public void put(SentenceForm form, GdlSentence sentence) {
		if (!containsSentence(form, sentence)) {
			sentences.put(form, sentence);
			if (!functionInfoMap.containsKey(form)) {
				functionInfoMap.put(form, MutableFunctionInfo.create(form));
			}
			functionInfoMap.get(form).addSentence(sentence);
		}
	}

	/**
	 * Returns an unmodifiable view of the function information
	 */
	public Map<SentenceForm, AddibleFunctionInfo> getFunctionInfo() {
		return Collections.unmodifiableMap(functionInfoMap);
	}
}
