package org.ggp.base.util.statemachine.implementation.prover.result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;


public final class ProverResultParser
{

	private final static GdlConstant TRUE = GdlPool.getConstant("true");

	public List<Move> toMoves(Set<GdlSentence> results)
	{
		List<Move> moves = new ArrayList<Move>();
		for (GdlSentence result : results)
		{
			moves.add(new Move(result.get(1)));
		}

		return moves;
	}

	public List<Role> toRoles(List<GdlSentence> results)
	{
		List<Role> roles = new ArrayList<Role>();
		for (GdlSentence result : results)
		{
			GdlConstant name = (GdlConstant) result.get(0);
			roles.add(new Role(name));
		}

		return roles;
	}

	public MachineState toState(Set<GdlSentence> results)
	{
		Set<GdlSentence> trues = new HashSet<GdlSentence>();
		for (GdlSentence result : results)
		{
			trues.add(GdlPool.getRelation(TRUE, new GdlTerm[] { result.get(0) }));
		}
		return new MachineState(trues);
	}
}