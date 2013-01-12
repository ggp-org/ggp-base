package org.ggp.base.util.gdl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlTerm;


public class TermModel {
	Set<GdlConstant> constantValues = new HashSet<GdlConstant>();
	Map<String, List<TermModel>> functionalValues = new HashMap<String, List<TermModel>>();
	public TermModel () {
	}
	
	void setConstants(Set<GdlConstant> constants) {
		this.constantValues = constants;
	}

	public TermModel(TermModel other) {
		//Copy other term model without sharing any memory
		constantValues.addAll(other.constantValues);
		for(String key : other.functionalValues.keySet()) {
			functionalValues.put(key, copy(other.functionalValues.get(key)));
		}
	}

	public void addTerm(GdlTerm term) {
		if(term instanceof GdlConstant) {
			constantValues.add((GdlConstant) term);
		} else if(term instanceof GdlFunction) {
			GdlFunction function = (GdlFunction) term;
			//Add the function
			//Do we already have a function of this name and arity?
			int arity = function.arity();
			String name = function.getName().getValue();
			//If not, add it to the list
			if(!functionalValues.containsKey(name)) {
				List<TermModel> terms = new ArrayList<TermModel>(arity);
				for(int i = 0; i < arity; i++)
					terms.add(new TermModel());
				functionalValues.put(name, terms);
			}
			List<TermModel> terms = functionalValues.get(name);
			//Add to each term model its value
			for(int i = 0; i < arity; i++) {
				terms.get(i).addTerm(function.get(i));
			}
		} else {
			throw new RuntimeException("Description contains non-constant, non-function term " + term + " in a supposedly constant relation");
		}
	}
	

	public boolean inject(TermModel other) {
		//Return true if we add anything new
		boolean changedSomething = false;
		
		if(constantValues.addAll(other.constantValues))
			changedSomething = true;
		for(Entry<String, List<TermModel>> function : other.functionalValues.entrySet()) {
			//If we don't have the function, add it and a copy of its contents
			String functionKey = function.getKey();
			List<TermModel> functionBody = function.getValue();
			if(!functionalValues.containsKey(functionKey)) {
				functionalValues.put(functionKey, copy(functionBody));
				changedSomething = true;
			} else {
				List<TermModel> ourFunction = functionalValues.get(functionKey);
				//Inject all the inner terms?
				for(int i = 0; i < ourFunction.size(); i++) {
					if(ourFunction.get(i).inject(functionBody.get(i)))
						changedSomething = true;
				}
			}
		}
		
		return changedSomething;
	}


	public boolean containsConstant(GdlConstant constant) {
		return constantValues.contains(constant);
	}
	public void addConstant(GdlConstant constant) {
		constantValues.add(constant);
	}

	public boolean containsFunction(String functionKey) {
		return functionalValues.containsKey(functionKey);
	}
	public List<TermModel> getFunction(String functionKey) {
		return functionalValues.get(functionKey);
	}
	public void addFunction(String functionName, int arity) {
		if(functionalValues.containsKey(functionName))
			throw new RuntimeException("Trying to add already-existing function name " + functionName);
		List<TermModel> termsList = new ArrayList<TermModel>(arity);
		for(int i = 0; i < arity; i++)
			termsList.add(new TermModel());
		functionalValues.put(functionName, termsList);
	}



	@Override
	public String toString() {
		String result = "{";
		for(GdlConstant c : constantValues) {
			result += c.getValue() + ", ";
		}
		for(Entry<String, List<TermModel>> fn : functionalValues.entrySet()) {
			result += "(" + fn.getKey();
			for(TermModel term : fn.getValue())
				result += " " + term;
			result += "), ";
		}
		result += "}";
		return result;
	}

	public boolean hasFunctions() {
		return !functionalValues.isEmpty();
	}

	public List<TermModel> getFunction(GdlFunction function) {
		return getFunction(function.getName().getValue());
	}

	public Set<GdlConstant> getConstants() {
		return constantValues;
	}

	public Map<String, List<TermModel>> getFunctions() {
		return functionalValues;
	}

	public static TermModel getIntersection(Collection<TermModel> list) {
		if(list.isEmpty())
			throw new RuntimeException("Looking for the intersection of an empty set of TermModels");
		
		Iterator<TermModel> itr = list.iterator();
		TermModel intersection = new TermModel(itr.next());
		
		while(itr.hasNext()) {
			TermModel cur = itr.next();
			//Intersect cur into the current intersection
			intersection.intersect(cur);
		}
		return intersection;
	}
	
	public static List<TermModel> copy(List<TermModel> body) {
		List<TermModel> copy = new ArrayList<TermModel>();
		for(TermModel t : body) {
			copy.add(new TermModel(t));
		}
		return copy;
	}

	private void intersect(TermModel other) {
		//Keep only the constants that are in both.
		//Keep a function only if it appears in both;
		//intersect each of the term models in the models
		constantValues.retainAll(other.constantValues);
		Set<String> keys = new HashSet<String>(functionalValues.keySet());
		for(String key : keys) {
			if(!other.containsFunction(key)) {
				functionalValues.remove(key);
			} else {
				//Intersect each term of the function's body with the other function
				List<TermModel> ourBody = functionalValues.get(key);
				List<TermModel> theirBody = other.functionalValues.get(key);
				for(int i = 0; i < ourBody.size(); i++) {
					ourBody.get(i).intersect(theirBody.get(i));
				}
			}
		}
	}
}