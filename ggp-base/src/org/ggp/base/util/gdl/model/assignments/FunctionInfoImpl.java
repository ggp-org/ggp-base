package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.transforms.ConstantChecker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

//Represents information about a sentence form that is constant.
public class FunctionInfoImpl implements FunctionInfo {
	private SentenceForm form;

	//True iff the slot has at most one value given the other slots' values
	private List<Boolean> dependentSlots = new ArrayList<Boolean>();
	private List<Map<ImmutableList<GdlConstant>, GdlConstant>> valueMaps = Lists.newArrayList();

	public FunctionInfoImpl(SentenceForm form, Set<GdlSentence> trueSentences) throws InterruptedException {
		this.form = form;

		int numSlots = form.getTupleSize();

		for(int i = 0; i < numSlots; i++) {
			//We want to establish whether or not this is a constant...
			Map<ImmutableList<GdlConstant>, GdlConstant> functionMap = Maps.newHashMap();
			boolean functional = true;
			for (GdlSentence sentence : trueSentences) {
				ConcurrencyUtils.checkForInterruption();
				List<GdlConstant> tuple = GdlUtils.getTupleFromGroundSentence(sentence);
				List<GdlConstant> tuplePart = Lists.newArrayListWithCapacity(tuple.size() - 1);
				tuplePart.addAll(tuple.subList(0, i));
				tuplePart.addAll(tuple.subList(i + 1, tuple.size()));
				if(functionMap.containsKey(tuplePart)) {
					//We have two tuples with different values in just this slot
					functional = false;
					break;
				}
				//Otherwise, we record it
				functionMap.put(ImmutableList.copyOf(tuplePart), tuple.get(i));
			}

			if(functional) {
				//Record the function
				dependentSlots.add(true);
				valueMaps.add(functionMap);
			} else {
				//Forget it
				dependentSlots.add(false);
				valueMaps.add(null);
			}
		}
	}


	@Override
	public Map<ImmutableList<GdlConstant>, GdlConstant> getValueMap(int index) {
		return valueMaps.get(index);
	}

	@Override
	public List<Boolean> getDependentSlots() {
		return dependentSlots;
	}

	/**
	 * Given a sentence of the constant form's sentence form, finds all
	 * the variables in the sentence that can be produced functionally.
	 *
	 * Note the corner case: If a variable appears twice in a sentence,
	 * it CANNOT be produced in this way.
	 */
	@Override
	public Set<GdlVariable> getProducibleVars(GdlSentence sentence) {
		if(!form.matches(sentence))
			throw new RuntimeException("Sentence "+sentence+" does not match constant form");
		List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);

		Set<GdlVariable> candidateVars = new HashSet<GdlVariable>();
		//Variables that appear multiple times go into multipleVars
		Set<GdlVariable> multipleVars = new HashSet<GdlVariable>();
		//...which, of course, means we have to spot non-candidate vars
		Set<GdlVariable> nonCandidateVars = new HashSet<GdlVariable>();

		for(int i = 0; i < tuple.size(); i++) {
			GdlTerm term = tuple.get(i);
			if(term instanceof GdlVariable
					&& !multipleVars.contains(term)) {
				GdlVariable var = (GdlVariable) term;
				if(candidateVars.contains(var)
						|| nonCandidateVars.contains(var)) {
					multipleVars.add(var);
					candidateVars.remove(var);
				} else if(dependentSlots.get(i)) {
					candidateVars.add(var);
				} else {
					nonCandidateVars.add(var);
				}
			}
		}

		return candidateVars;

	}
	public static FunctionInfo create(SentenceForm form,
			ConstantChecker constantChecker) throws InterruptedException {
		return new FunctionInfoImpl(form, ImmutableSet.copyOf(constantChecker.getTrueSentences(form)));
	}
	public static FunctionInfo create(SentenceForm form,
			Set<GdlSentence> set) throws InterruptedException {
		return new FunctionInfoImpl(form, set);
	}
	@Override
	public SentenceForm getSentenceForm() {
		return form;
	}
	@Override
	public String toString() {
		return "FunctionInfoImpl [form=" + form + ", dependentSlots="
				+ dependentSlots + ", valueMaps=" + valueMaps + "]";
	}
}