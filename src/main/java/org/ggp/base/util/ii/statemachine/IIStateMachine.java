package org.ggp.base.util.ii.statemachine;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public interface IIStateMachine {
    MachineState getInitialState();

    List<Role> getRoles();

    IIStateView getViewOfState(MachineState state, Role role);

    List<Move> getLegalMovesForRole(MachineState state, Role role) throws MoveDefinitionException;

    List<Move> getLegalMovesForRole(IIStateView stateViewForRole) throws MoveDefinitionException;

    boolean isTerminal(MachineState state);

    int getGoalValue(MachineState state, Role role) throws GoalDefinitionException;

    List<Integer> getGoalValues(MachineState state) throws GoalDefinitionException;

    MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException;
}
