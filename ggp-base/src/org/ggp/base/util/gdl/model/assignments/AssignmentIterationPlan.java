package org.ggp.base.util.gdl.model.assignments;

import java.util.List;
import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class AssignmentIterationPlan {
	//TODO: Come up with better representations
	private final ImmutableList<GdlVariable> varsToAssign;
	private final ImmutableList<ImmutableList<ImmutableList<GdlConstant>>> tuplesBySource;
	private final ImmutableMap<GdlVariable, GdlConstant> headAssignment;
	private final ImmutableList<Integer> indicesToChangeWhenNull;
	private final ImmutableList<GdlDistinct> distincts;
	private final ImmutableMap<Integer, GdlVariable> varsToChangePerDistinct;
	private final ImmutableMap<Integer, AssignmentFunction> valuesToCompute;
	private final ImmutableList<Integer> sourceDefiningSlot;
	private final ImmutableList<ImmutableList<GdlConstant>> valuesToIterate;
	private final ImmutableList<ImmutableList<Integer>> varsChosenBySource;
	private final ImmutableList<ImmutableList<Boolean>> putDontCheckBySource;
	private final boolean empty;
	private final boolean allDone;

	public AssignmentIterationPlan(
			ImmutableList<GdlVariable> varsToAssign,
			ImmutableList<ImmutableList<ImmutableList<GdlConstant>>> tuplesBySource,
			ImmutableMap<GdlVariable, GdlConstant> headAssignment,
			ImmutableList<Integer> indicesToChangeWhenNull,
			ImmutableList<GdlDistinct> distincts,
			ImmutableMap<Integer, GdlVariable> varsToChangePerDistinct,
			ImmutableMap<Integer, AssignmentFunction> valuesToCompute,
			ImmutableList<Integer> sourceDefiningSlot,
			ImmutableList<ImmutableList<GdlConstant>> valuesToIterate,
			ImmutableList<ImmutableList<Integer>> varsChosenBySource,
			ImmutableList<ImmutableList<Boolean>> putDontCheckBySource,
			boolean empty,
			boolean allDone) {
		this.varsToAssign = varsToAssign;
		this.tuplesBySource = tuplesBySource;
		this.headAssignment = headAssignment;
		this.indicesToChangeWhenNull = indicesToChangeWhenNull;
		this.distincts = distincts;
		this.varsToChangePerDistinct = varsToChangePerDistinct;
		this.valuesToCompute = valuesToCompute;
		this.sourceDefiningSlot = sourceDefiningSlot;
		this.valuesToIterate = valuesToIterate;
		this.varsChosenBySource = varsChosenBySource;
		this.putDontCheckBySource = putDontCheckBySource;
		this.empty = empty;
		this.allDone = allDone;
	}

	public ImmutableList<GdlVariable> getVarsToAssign() {
		return varsToAssign;
	}

	public ImmutableList<ImmutableList<ImmutableList<GdlConstant>>> getTuplesBySource() {
		return tuplesBySource;
	}

	public ImmutableMap<GdlVariable, GdlConstant> getHeadAssignment() {
		return headAssignment;
	}

	public ImmutableList<Integer> getIndicesToChangeWhenNull() {
		return indicesToChangeWhenNull;
	}

	public ImmutableList<GdlDistinct> getDistincts() {
		return distincts;
	}

	public ImmutableMap<Integer, GdlVariable> getVarsToChangePerDistinct() {
		return varsToChangePerDistinct;
	}

	public ImmutableMap<Integer, AssignmentFunction> getValuesToCompute() {
		return valuesToCompute;
	}

	public ImmutableList<Integer> getSourceDefiningSlot() {
		return sourceDefiningSlot;
	}

	public ImmutableList<ImmutableList<GdlConstant>> getValuesToIterate() {
		return valuesToIterate;
	}

	public ImmutableList<ImmutableList<Integer>> getVarsChosenBySource() {
		return varsChosenBySource;
	}

	public ImmutableList<ImmutableList<Boolean>> getPutDontCheckBySource() {
		return putDontCheckBySource;
	}

	public boolean getEmpty() {
		return empty;
	}

	public boolean getAllDone() {
		return allDone;
	}

	private static final AssignmentIterationPlan EMPTY_ITERATION_PLAN =
			new AssignmentIterationPlan(
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					true,
					false
					);

	public static AssignmentIterationPlan create(List<GdlVariable> varsToAssign,
			List<ImmutableList<ImmutableList<GdlConstant>>> tuplesBySource,
			Map<GdlVariable, GdlConstant> headAssignment,
			List<Integer> indicesToChangeWhenNull,
			List<GdlDistinct> distincts,
			List<GdlVariable> varsToChangePerDistinct,
			List<AssignmentFunction> valuesToCompute,
			List<Integer> sourceDefiningSlot,
			List<ImmutableList<GdlConstant>> valuesToIterate,
			List<ImmutableList<Integer>> varsChosenBySource,
			List<ImmutableList<Boolean>> putDontCheckBySource,
			boolean empty,
			boolean allDone) {
		if (empty) {
			return EMPTY_ITERATION_PLAN;
		}
		return new AssignmentIterationPlan(ImmutableList.copyOf(varsToAssign),
				ImmutableList.copyOf(tuplesBySource),
				ImmutableMap.copyOf(headAssignment),
				ImmutableList.copyOf(indicesToChangeWhenNull),
				ImmutableList.copyOf(distincts),
				fromNullableList(varsToChangePerDistinct),
				fromNullableList(valuesToCompute),
				ImmutableList.copyOf(sourceDefiningSlot),
				ImmutableList.copyOf(valuesToIterate),
				ImmutableList.copyOf(varsChosenBySource),
				ImmutableList.copyOf(putDontCheckBySource),
				empty,
				allDone);
	}

	private static <T> ImmutableMap<Integer, T> fromNullableList(
			List<T> nullableList) {
		ImmutableMap.Builder<Integer, T> builder = ImmutableMap.builder();
		for (int i = 0; i < nullableList.size(); i++) {
			if (nullableList.get(i) != null) {
				builder.put(i, nullableList.get(i));
			}
		}
		return builder.build();
	}
}
