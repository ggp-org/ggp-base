package org.ggp.base.util.gdl.model;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.w3c.tidy.MutableInteger;


public class SimpleSentenceForm implements SentenceForm {
	private final GdlConstant name;
	private final List<GdlConstant> functionNames;
	private final List<Integer> functionIndices; // Tuple index
	private final List<Integer> functionArities;
	private final int tupleSize;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((functionArities == null) ? 0 : functionArities.hashCode());
		result = prime * result
				+ ((functionIndices == null) ? 0 : functionIndices.hashCode());
		result = prime * result
				+ ((functionNames == null) ? 0 : functionNames.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + tupleSize;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleSentenceForm other = (SimpleSentenceForm) obj;
		if (functionArities == null) {
			if (other.functionArities != null)
				return false;
		} else if (!functionArities.equals(other.functionArities))
			return false;
		if (functionIndices == null) {
			if (other.functionIndices != null)
				return false;
		} else if (!functionIndices.equals(other.functionIndices))
			return false;
		if (functionNames == null) {
			if (other.functionNames != null)
				return false;
		} else if (!functionNames.equals(other.functionNames))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (tupleSize != other.tupleSize)
			return false;
		return true;
	}

	public SimpleSentenceForm(GdlSentence sentence) {
		name = sentence.getName();
		functionNames = new ArrayList<GdlConstant>();
		functionIndices = new ArrayList<Integer>();
		functionArities = new ArrayList<Integer>();
		
		MutableInteger indexRef = new MutableInteger();
		indexRef.value = 0;
		crawlBody(sentence.getBody(), indexRef);
		tupleSize = indexRef.value;
	}

	public SimpleSentenceForm(GdlConstant newName,
			SimpleSentenceForm other) {
		name = newName;
		functionNames = other.functionNames;
		functionIndices = other.functionIndices;
		functionArities = other.functionArities;
		tupleSize = other.tupleSize;
	}

	private void crawlBody(List<GdlTerm> body, MutableInteger indexRef) {
		for (GdlTerm term : body) {
			if (term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				functionNames.add(function.getName());
				functionIndices.add(indexRef.value);
				functionArities.add(function.arity());
				crawlBody(function.getBody(), indexRef);
			}
			++indexRef.value;
		}
	}

	@Override
	public GdlConstant getName() {
		return name;
	}

	@Override
	public SentenceForm getCopyWithName(GdlConstant name) {
		return new SimpleSentenceForm(name, this);
	}

	@Override
	public boolean matches(GdlSentence sentence) {
		if (!sentence.getName().equals(name)) {
			return false;
		}
		MutableInteger indexRef = new MutableInteger();
		indexRef.value = 0;
		MutableInteger functionIndexRef = new MutableInteger();
		functionIndexRef.value = 0;
		
		return bodyMatches(sentence.getBody(), indexRef, functionIndexRef);
	}

	private boolean bodyMatches(List<GdlTerm> body,
			MutableInteger indexRef, MutableInteger functionIndexRef) {
		for(GdlTerm term : body) {
			if (term instanceof GdlFunction) {
				//If this isn't the right function, return false
				GdlFunction function = (GdlFunction) term;
				if (functionIndices.get(functionIndexRef.value) != indexRef.value
						|| !functionNames.get(functionIndexRef.value).equals(function.getName())
						|| functionArities.get(functionIndexRef.value) != function.arity()) {
					return false;
				}
				++functionIndexRef.value;
				if (!bodyMatches(function.getBody(), indexRef, functionIndexRef)) {
					return false;
				}
			} else {
				//If we expect a function here, return false
				if (functionIndices.get(functionIndexRef.value) == indexRef.value) {
					return false;
				}
			}
			++indexRef.value;
		}
		return true;
	}

	@Override
	public int getTupleSize() {
		return tupleSize;
	}
}
