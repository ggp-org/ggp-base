package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceForm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Defines a {@link FunctionInfo} that can have values added to it (but
 * not removed from it) over time. This allows the functional
 * info to stay correct as new values are added with minimal
 * additional computation.
 *
 * Not thread-safe.
 */
public class MutableFunctionInfo implements AddibleFunctionInfo {
	private final SentenceForm form;
	private final List<Boolean> dependentSlots = new ArrayList<Boolean>();
	private final List<Map<ImmutableList<GdlConstant>, GdlConstant>> valueMaps = Lists.newArrayList();

	private MutableFunctionInfo(SentenceForm form) {
		this.form = form;
		for (int i = 0; i < form.getTupleSize(); i++) {
			dependentSlots.add(true);
			valueMaps.add(Maps.<ImmutableList<GdlConstant>, GdlConstant>newHashMap());
		}
	}

	public static MutableFunctionInfo create(SentenceForm form) {
		return create(form, ImmutableSet.<GdlSentence>of());
	}

	public static MutableFunctionInfo create(SentenceForm form, Collection<GdlSentence> initialSentences) {
		MutableFunctionInfo functionInfo = new MutableFunctionInfo(form);
		for (GdlSentence sentence : initialSentences) {
			functionInfo.addTuple(GdlUtils.getTupleFromGroundSentence(sentence));
		}
		return functionInfo;
	}

	@Override
	public SentenceForm getSentenceForm() {
		return form;
	}

	@Override
	public void addTuple(List<GdlConstant> sentenceTuple) {
		if (sentenceTuple.size() != form.getTupleSize()) {
			throw new IllegalArgumentException();
		}
		//For each slot...
		for (int i = 0; i < sentenceTuple.size(); i++) {
			if (dependentSlots.get(i)) {
				//Either add to that entry, or invalidate the slot
				Map<ImmutableList<GdlConstant>, GdlConstant> valueMap = valueMaps.get(i);
				List<GdlConstant> lookupTuple = Lists.newArrayList(sentenceTuple);
				lookupTuple.remove(i);

				GdlConstant curValue = valueMap.get(lookupTuple);
				GdlConstant newValue = sentenceTuple.get(i);
				if (curValue == null) {
					//Just add to the map
					valueMap.put(ImmutableList.copyOf(lookupTuple), newValue);
				} else {
					//If this isn't the existing sentence, invalidate this slot
					if (curValue != newValue) {
						dependentSlots.set(i, false);
						valueMaps.set(i, ImmutableMap.<ImmutableList<GdlConstant>, GdlConstant>of());
					}
				}
			}
		}
	}

	@Override
	public List<Boolean> getDependentSlots() {
		return Collections.unmodifiableList(dependentSlots);
	}

	@Override
	public Set<GdlVariable> getProducibleVars(GdlSentence sentence) {
		return FunctionInfos.getProducibleVars(this, sentence);
	}

	@Override
	public Map<ImmutableList<GdlConstant>, GdlConstant> getValueMap(int varIndex) {
		return Collections.unmodifiableMap(valueMaps.get(varIndex));
	}

	@Override
	public void addSentence(GdlSentence sentence) {
		addTuple(GdlUtils.getTupleFromGroundSentence(sentence));
	}

	@Override
	public String toString() {
		return "MutableFunctionInfo [form=" + form + ", dependentSlots="
				+ dependentSlots + ", valueMaps=" + valueMaps + "]";
	}
}
