package util.statemachine.implementation.prover.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.cache.TtlCache;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public final class CachedProverStateMachine extends ProverStateMachine
{

	private final class Entry
	{
		public Map<Role, Integer> goals;
		public Map<Role, List<Move>> moves;
		public Map<List<Move>, MachineState> nexts;
		public Boolean terminal;

		public Entry()
		{
			goals = new HashMap<Role, Integer>();
			moves = new HashMap<Role, List<Move>>();
			nexts = new HashMap<List<Move>, MachineState>();
			terminal = null;
		}
	}

	private final TtlCache<MachineState, Entry> ttlCache;

	public CachedProverStateMachine()
	{
		ttlCache = new TtlCache<MachineState, Entry>(1);
	}

	private Entry getEntry(MachineState state)
	{
		if (!ttlCache.containsKey(state))
		{
			ttlCache.put(state, new Entry());
		}

		return ttlCache.get(state);
	}

	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException
	{
		Entry entry = getEntry(state);
		synchronized (entry)
		{
			if (!entry.goals.containsKey(role))
			{
				entry.goals.put(role, super.getGoal(state, role));
			}

			return entry.goals.get(role);
		}
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
	{
		Entry entry = getEntry(state);
		synchronized (entry)
		{
			if (!entry.moves.containsKey(role))
			{
				entry.moves.put(role, super.getLegalMoves(state, role));
			}

			return entry.moves.get(role);
		}
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
	{
		Entry entry = getEntry(state);
		synchronized (entry)
		{
			if (!entry.nexts.containsKey(moves))
			{
				entry.nexts.put(moves, super.getNextState(state, moves));
			}

			return entry.nexts.get(moves);
		}
	}

	@Override
	public boolean isTerminal(MachineState state)
	{
		Entry entry = getEntry(state);
		synchronized (entry)
		{
			if (entry.terminal == null)
			{
				entry.terminal = super.isTerminal(state);
			}

			return entry.terminal;
		}
	}
	
	@Override
	public void doPerMoveWork()
	{
		prune();
	}

	public void prune()
	{
		ttlCache.prune();
	}

}
