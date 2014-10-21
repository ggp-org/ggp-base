package org.ggp.base.util.gdl.model.assignments;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

public class FunctionInfos {
	public static Set<GdlVariable> getProducibleVars(FunctionInfo functionInfo, GdlSentence sentence) {
		if (!functionInfo.getSentenceForm().matches(sentence)) {
			throw new RuntimeException("Sentence "+sentence+" does not match constant form");
		}

		List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
		List<Boolean> dependentSlots = functionInfo.getDependentSlots();

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
}
