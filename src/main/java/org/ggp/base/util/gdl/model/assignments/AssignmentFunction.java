package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class AssignmentFunction {
		//How is the AssignmentFunction going to operate?
		//Well, some of the variables are going to be
		//specified as having one or more of these functions
		//apply to them. (If multiple apply, they all have
		//to agree.)
		//We pass in the current value of the tuple and
		//it gives us the value desired (or null).
		//This means it just has to know which indices in
		//the tuple (i.e. which variables) correspond to
		//which slots in its native tuple.

		//Used when multiple assignment functions are relevant
		//to the same variable. In this case we call these other
		//functions with the same arguments and return null if
		//any of the answers differ.
		private final ImmutableList<AssignmentFunction> internalFunctions;
		private final int querySize;
		private final ImmutableList<Boolean> isInputConstant;
		private final ImmutableMap<Integer, GdlConstant> queryConstants;
		private final ImmutableList<Integer> queryInputIndices;
		private final ImmutableMap<ImmutableList<GdlConstant>, GdlConstant> function;
		//Some sort of trie might work better here...

		private AssignmentFunction(ImmutableList<AssignmentFunction> internalFunctions,
				int querySize,
				ImmutableList<Boolean> isInputConstant,
				ImmutableMap<Integer, GdlConstant> queryConstants,
				ImmutableList<Integer> queryInputIndices,
				ImmutableMap<ImmutableList<GdlConstant>, GdlConstant> function) {
			this.internalFunctions = internalFunctions;
			this.querySize = querySize;
			this.isInputConstant = isInputConstant;
			this.queryConstants = queryConstants;
			this.queryInputIndices = queryInputIndices;
			this.function = function;
		}

		public static AssignmentFunction create(GdlRelation conjunct, FunctionInfo functionInfo,
				GdlVariable rightmostVar, List<GdlVariable> varOrder,
				Map<GdlVariable, GdlConstant> preassignment) {
			//We have to set up the things mentioned above...
			List<AssignmentFunction> internalFunctions = new ArrayList<AssignmentFunction>();

			//We can traverse the conjunct for the list of variables/constants...
			List<GdlTerm> terms = new ArrayList<GdlTerm>();
			gatherVars(conjunct.getBody(), terms);
			//Note that we assume here that the var of interest only
			//appears once in the relation...
			int varIndex = terms.indexOf(rightmostVar);
			if(varIndex == -1) {
				System.out.println("conjunct is: " + conjunct);
				System.out.println("terms are: " + terms);
				System.out.println("righmostVar is: " + rightmostVar);
			}
			terms.remove(rightmostVar);
			Map<ImmutableList<GdlConstant>, GdlConstant> function = functionInfo.getValueMap(varIndex);

			//Set up inputs and such, using terms
			int querySize = terms.size();
			List<Boolean> isInputConstant = new ArrayList<Boolean>(terms.size());
			Map<Integer, GdlConstant> queryConstants = Maps.newHashMap();
			List<Integer> queryInputIndices = new ArrayList<Integer>(terms.size());
			for(int i = 0; i < terms.size(); i++) {
				GdlTerm term = terms.get(i);
				if(term instanceof GdlConstant) {
					isInputConstant.add(true);
					queryConstants.put(i, (GdlConstant) term);
					queryInputIndices.add(-1);
				} else if(term instanceof GdlVariable) {
					//Is it in the head assignment?
					if(preassignment.containsKey(term)) {
						isInputConstant.add(true);
						queryConstants.put(i, preassignment.get(term));
						queryInputIndices.add(-1);
					} else {
						isInputConstant.add(false);
//						queryConstants.add(null);
						//What value do we put here?
						//We want to grab some value out of the
						//input tuple, which uses functional ordering
						//Index of the relevant variable, by the
						//assignment's ordering
						queryInputIndices.add(varOrder.indexOf(term));
					}
				}
			}
			return new AssignmentFunction(
					ImmutableList.copyOf(internalFunctions),
					querySize,
					ImmutableList.copyOf(isInputConstant),
					ImmutableMap.copyOf(queryConstants),
					ImmutableList.copyOf(queryInputIndices),
					ImmutableMap.copyOf(function));
		}

		public boolean functional() {
			return (function != null);
		}

		private static void gatherVars(List<GdlTerm> body, List<GdlTerm> terms) {
			for(GdlTerm term : body) {
				if(term instanceof GdlConstant || term instanceof GdlVariable)
					terms.add(term);
				else if(term instanceof GdlFunction)
					gatherVars(((GdlFunction)term).getBody(), terms);
			}
		}

		public GdlConstant getValue(List<GdlConstant> remainingTuple) {
			//We have a map from a tuple of GdlConstants
			//to the GdlConstant we need, provided by the FunctionInfo.
			//We need to make the tuple for this map.
			List<GdlConstant> queryTuple = new ArrayList<GdlConstant>(querySize);
			//Now we have to fill in the query
			for(int i = 0; i < querySize; i++) {
				if(isInputConstant.get(i)) {
					queryTuple.add(queryConstants.get(i));
				} else {
					queryTuple.add(remainingTuple.get(queryInputIndices.get(i)));
				}
			}
			//The query is filled; we ask the map
			GdlConstant answer = function.get(queryTuple);

			for (AssignmentFunction internalFunction : internalFunctions) {
				if (internalFunction.getValue(remainingTuple) != answer) {
					return null;
				}
			}
			return answer;
		}
	}