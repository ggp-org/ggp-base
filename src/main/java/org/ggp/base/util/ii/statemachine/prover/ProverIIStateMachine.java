package org.ggp.base.util.ii.statemachine.prover;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.ii.statemachine.IIStateMachine;
import org.ggp.base.util.ii.statemachine.IIStateView;
import org.ggp.base.util.ii.statemachine.SimpleIIStateView;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;
import org.ggp.base.util.statemachine.implementation.prover.result.ProverResultParser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class ProverIIStateMachine implements IIStateMachine {
    private final MachineState initialState;
    private final Prover prover;
    private final ImmutableList<Role> roles;

    private ProverIIStateMachine(MachineState initialState, Prover prover, ImmutableList<Role> roles) {
        this.initialState = initialState;
        this.prover = prover;
        this.roles = roles;
    }

    public static ProverIIStateMachine create(List<Gdl> rules) {
        Prover prover = new AimaProver(rules);
        ImmutableList<Role> roles = ImmutableList.copyOf(Role.computeRoles(rules));
        MachineState initialState = computeInitialState(prover);
        return new ProverIIStateMachine(initialState, prover, roles);
    }

    private static MachineState computeInitialState(Prover prover) {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getInitQuery(), new HashSet<GdlSentence>());
        return new ProverResultParser().toState(results);
    }

    @Override
    public MachineState getInitialState() {
        return initialState;
    }

    @Override
    public List<Role> getRoles() {
        return roles;
    }

    @Override
    public IIStateView getViewOfState(MachineState state, Role role) {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getSeesQuery(role), ProverQueryBuilder.getContext(state));

        return new SimpleIIStateView(ImmutableSet.copyOf(results), role);
    }

    @Override
    public List<Move> getLegalMovesForRole(MachineState state, Role role) throws MoveDefinitionException {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getLegalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.isEmpty())
        {
            throw new MoveDefinitionException(state, role);
        }

        return new ProverResultParser().toMoves(results);
    }

    @Override
    public List<Move> getLegalMovesForRole(IIStateView stateView) throws MoveDefinitionException {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getLegalQuery(stateView.getRole()), ProverQueryBuilder.getContext(stateView));

        if (results.isEmpty())
        {
            throw new MoveDefinitionException(stateView);
        }

        return new ProverResultParser().toMoves(results);
    }

    @Override
    public boolean isTerminal(MachineState state) {
        return prover.prove(ProverQueryBuilder.getTerminalQuery(), ProverQueryBuilder.getContext(state));
    }

    @Override
    public int getGoalValue(MachineState state, Role role) throws GoalDefinitionException {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getGoalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.size() != 1)
        {
            GamerLogger.logError("StateMachine", "Got goal results of size: " + results.size() + " when expecting size one.");
            throw new GoalDefinitionException(state, role);
        }

        try
        {
            GdlRelation relation = (GdlRelation) results.iterator().next();
            GdlConstant constant = (GdlConstant) relation.get(1);

            return Integer.parseInt(constant.toString());
        }
        catch (Exception e)
        {
            throw new GoalDefinitionException(state, role);
        }
    }

    @Override
    public List<Integer> getGoalValues(MachineState state) throws GoalDefinitionException {
        List<Integer> goals = Lists.newArrayListWithCapacity(roles.size());
        for (Role role : roles) {
            goals.add(getGoalValue(state, role));
        }
        return goals;
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getNextQuery(), ProverQueryBuilder.getContext(state, getRoles(), moves));

        for (GdlSentence sentence : results)
        {
            if (!sentence.isGround())
            {
                throw new TransitionDefinitionException(state, moves);
            }
        }

        return new ProverResultParser().toState(results);
    }

}
