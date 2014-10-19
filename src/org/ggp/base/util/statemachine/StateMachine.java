package org.ggp.base.util.statemachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import com.google.common.collect.ImmutableMap;


/**
 * Provides the base class for all state machine implementations.
 */
public abstract class StateMachine
{
    // ============================================
    //          Stubs for implementations
    // ============================================
    //  The following methods are required for a valid
    // state machine implementation.
	/**
	 * Initializes the StateMachine to describe the given game rules.
	 * <p>
	 * This method should only be called once, and it should be called before any
	 * other methods on the StateMachine.
	 */
    public abstract void initialize(List<Gdl> description);
    /**
     * Returns the goal value for the given role in the given state. Goal values
     * are always between 0 and 100.
     *
     * @throws GoalDefinitionException if there is no goal value or more than one
     * goal value for the given role in the given state. If this occurs when this
     * is called on a terminal state, this indicates an error in either the game
     * description or the StateMachine implementation.
     */
    public abstract int getGoal(MachineState state, Role role) throws GoalDefinitionException;
    /**
     * Returns true if and only if the given state is a terminal state (i.e. the
     * game is over).
     */
    public abstract boolean isTerminal(MachineState state);

    /**
     * Returns a list of the roles in the game, in the same order as they
     * were defined in the game description.
     * <p>
     * The result will be the same as calling {@link Role#computeRoles(List)}
     * on the game rules used to initialize this state machine.
     */
    public abstract List<Role> getRoles();
    /**
     * Returns the initial state of the game.
     */
    public abstract MachineState getInitialState();

    /**
     * Returns a list containing every move that is legal for the given role in the
     * given state.
     *
     * @throws MoveDefinitionException if the role has no legal moves. This indicates
     * an error in either the game description or the StateMachine implementation.
     */
    // TODO: There are philosophical reasons for this to return Set<Move> rather than List<Move>.
    public abstract List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException;

    /**
     * Returns the next state of the game given the current state and a joint move
     * list containing one move per role.
     *
     * @param moves A list containing one move per role. The moves should be
     * listed in the same order as roles are listed by {@link #getRoles()}.
     * @throws TransitionDefinitionException indicates an error in either the
     * game description or the StateMachine implementation.
     */
    public abstract MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException;

    // The following methods are included in the abstract StateMachine base so
    // implementations which use alternative Role/Move/State representations
    // can look up/compute what some Gdl corresponds to in their representation.
    // They are implemented for convenience, using the default ways of generating
    // these objects, but they can be overridden to support machine-specific objects.
    public MachineState getMachineStateFromSentenceList(Set<GdlSentence> sentenceList) {
        return new MachineState(sentenceList);
    }
    public Role getRoleFromConstant(GdlConstant constant) {
        return new Role(constant);
    }
    public Move getMoveFromTerm(GdlTerm term) {
        return new Move(term);
    }

    // ============================================
    //          Stubs for advanced methods
    // ============================================
    //
    //   The following methods have functioning stubs,
    // which can be overridden with full-fledged versions
    // as needed by state machines. Clients should assume
    // the contracts for these methods hold, regardless
    // of the state machine implementation they pick.

    /** Override this to perform some extra work (like trimming a cache) once per move.
     * <p>
     * CONTRACT: Should be called once per move.
     */
    public void doPerMoveWork() {}

    /** Override this to provide memory-saving destructive-next-state functionality.
     * <p>
     * CONTRACT: After calling this method, "state" should not be accessed.
     */
    public MachineState getNextStateDestructively(MachineState state, List<Move> moves) throws TransitionDefinitionException {
        return getNextState(state, moves);
    }

    /** Override this to allow the state machine to be conditioned on a particular current state.
     * This means that the state machine will only handle portions of the game tree at and below
     * the given state; it no longer needs to properly handle earlier portions of the game tree.
     * This constraint can be used to optimize certain state machine implementations.
     * <p>
     * CONTRACT: After calling this method, the state machine never deals with a state that
     *           is not "theState" or one of its descendants in the game tree.
     */
    public void updateRoot(MachineState theState) {
        ;
    }

    // ============================================
    //   Implementations of convenience methods
    // ============================================

    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns a list containing every joint move possible in the given state.
     * A joint move consists of one move for each role, with the moves in the
     * same ordering that their roles have in {@link #getRoles()}.
     * <p>
     * The list of possible joint moves is the Cartesian product of the lists
     * of legal moves available for each player.
     * <p>
     * If only one player has more than one legal move, then the number of
     * joint moves returned will equal the number of possible moves for that
     * player.
     */
    public List<List<Move>> getLegalJointMoves(MachineState state) throws MoveDefinitionException
    {
        List<List<Move>> legals = new ArrayList<List<Move>>();
        for (Role role : getRoles()) {
            legals.add(getLegalMoves(state, role));
        }

        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());

        return crossProduct;
    }

    /**
     * Returns a list of every joint move possible in the given state in which
     * the given role makes the given move. This will be a subset of the list
     * of joint moves given by {@link #getLegalJointMoves(MachineState)}.
     */
    public List<List<Move>> getLegalJointMoves(MachineState state, Role role, Move move) throws MoveDefinitionException
    {
        List<List<Move>> legals = new ArrayList<List<Move>>();
        for (Role r : getRoles()) {
            if (r.equals(role)) {
                List<Move> m = new ArrayList<Move>();
                m.add(move);
                legals.add(m);
            } else {
                legals.add(getLegalMoves(state, r));
            }
        }

        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());

        return crossProduct;
    }

    /**
     * Returns a list containing every possible next state of the game after
     * the given state. The list will contain one entry for every possible
     * joint move that could be played; as such, a single machine state could
     * be included multiple times.
     */
    public List<MachineState> getNextStates(MachineState state) throws MoveDefinitionException, TransitionDefinitionException
    {
        List<MachineState> nextStates = new ArrayList<MachineState>();
        for (List<Move> move : getLegalJointMoves(state)) {
            nextStates.add(getNextState(state, move));
        }

        return nextStates;
    }

    /**
     * Returns a map from each move that is legal for the given role in
     * the given state to the list of possible resulting states if that
     * move is chosen.
     * <p>
     * If the given role is the only role with more than one legal move,
     * then each list of states in the map will only contain one state.
     */
    public Map<Move, List<MachineState>> getNextStates(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException
    {
        Map<Move, List<MachineState>> nextStates = new HashMap<Move, List<MachineState>>();
        Map<Role, Integer> roleIndices = getRoleIndices();
        for (List<Move> moves : getLegalJointMoves(state)) {
            Move move = moves.get(roleIndices.get(role));
            if (!nextStates.containsKey(move)) {
                nextStates.put(move, new ArrayList<MachineState>());
            }
            nextStates.get(move).add(getNextState(state, moves));
        }

        return nextStates;
    }

    protected void crossProductLegalMoves(List<List<Move>> legals, List<List<Move>> crossProduct, LinkedList<Move> partial)
    {
        if (partial.size() == legals.size()) {
            crossProduct.add(new ArrayList<Move>(partial));
        } else {
            for (Move move : legals.get(partial.size())) {
                partial.addLast(move);
                crossProductLegalMoves(legals, crossProduct, partial);
                partial.removeLast();
            }
        }
    }

    private Map<Role,Integer> roleIndices = null;
    /**
     * Returns a mapping from a role to the index of that role, as in
     * the list returned by {@link #getRoles()}. This may be a faster
     * way to check the index of a role than calling {@link List#indexOf(Object)}
     * on that list.
     */
    public Map<Role, Integer> getRoleIndices()
    {
        if (roleIndices == null) {
        	ImmutableMap.Builder<Role, Integer> roleIndicesBuilder = ImmutableMap.builder();
            List<Role> roles = getRoles();
            for (int i = 0; i < roles.size(); i++) {
                roleIndicesBuilder.put(roles.get(i), i);
            }
            roleIndices = roleIndicesBuilder.build();
        }

        return roleIndices;
    }

    /**
     * Returns the goal values for each role in the given state. The goal values
     * are listed in the same order the roles are listed in the game rules, which
     * is the same order in which they're returned by {@link #getRoles()}.
     *
     * @throws GoalDefinitionException if there is no goal value or more than one
     * goal value for any one role in the given state. If this occurs when this
     * is called on a terminal state, this indicates an error in either the game
     * description or the StateMachine implementation.
     */
    public List<Integer> getGoals(MachineState state) throws GoalDefinitionException {
        List<Integer> theGoals = new ArrayList<Integer>();
        for (Role r : getRoles()) {
            theGoals.add(getGoal(state, r));
        }
        return theGoals;
    }

    /**
     * Returns a random joint move from among all the possible joint moves in
     * the given state.
     */
    public List<Move> getRandomJointMove(MachineState state) throws MoveDefinitionException
    {
        List<Move> random = new ArrayList<Move>();
        for (Role role : getRoles()) {
            random.add(getRandomMove(state, role));
        }

        return random;
    }

    /**
     * Returns a random joint move from among all the possible joint moves in
     * the given state in which the given role makes the given move.
     */
    public List<Move> getRandomJointMove(MachineState state, Role role, Move move) throws MoveDefinitionException
    {
        List<Move> random = new ArrayList<Move>();
        for (Role r : getRoles()) {
            if (r.equals(role)) {
                random.add(move);
            } else {
                random.add(getRandomMove(state, r));
            }
        }

        return random;
    }

    /**
     * Returns a random move from among the possible legal moves for the
     * given role in the given state.
     */
    public Move getRandomMove(MachineState state, Role role) throws MoveDefinitionException
    {
        List<Move> legals = getLegalMoves(state, role);
        return legals.get(new Random().nextInt(legals.size()));
    }

    /**
     * Returns a state chosen at random from the possible next states of the
     * game.
     * <p>
     * The distribution among states is based on the possible joint moves.
     * This is not necessarily uniform among the possible states themselves,
     * as multiple joint moves may result in the same state.
     */
    public MachineState getRandomNextState(MachineState state) throws MoveDefinitionException, TransitionDefinitionException
    {
        List<Move> random = getRandomJointMove(state);
        return getNextState(state, random);
    }

    /**
     * Returns a random next state of the game from the possible next states
     * resulting from the given role playing the given move.
     * <p>
     * The distribution among states is based on the possible joint moves.
     * This is not necessarily uniform among the possible states themselves,
     * as multiple joint moves may result in the same state.
     * <p>
     * If the given role is the only role with more than one legal move, then
     * there is only one possible next state for this method to return.
     */
    public MachineState getRandomNextState(MachineState state, Role role, Move move) throws MoveDefinitionException, TransitionDefinitionException
    {
        List<Move> random = getRandomJointMove(state, role, move);
        return getNextState(state, random);
    }

    /**
     * Returns a terminal state derived from repeatedly making random joint moves
     * until reaching the end of the game.
     *
     * @param theDepth an integer array, the 0th element of which will be set to
     * the number of state changes that were made to reach a terminal state.
     */
    public MachineState performDepthCharge(MachineState state, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
        int nDepth = 0;
        while(!isTerminal(state)) {
            nDepth++;
            state = getNextStateDestructively(state, getRandomJointMove(state));
        }
        if(theDepth != null)
            theDepth[0] = nDepth;
        return state;
    }

    public void getAverageDiscountedScoresFromRepeatedDepthCharges(final MachineState state, final double[] avgScores, final double[] avgDepth, final double discountFactor, final int repetitions) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	avgDepth[0] = 0;
    	for (int j = 0; j < avgScores.length; j++) {
    		avgScores[j] = 0;
    	}
    	final int[] depth = new int[1];
    	for (int i = 0; i < repetitions; i++) {
    		MachineState stateForCharge = state.clone();
    		stateForCharge = performDepthCharge(stateForCharge, depth);
    		avgDepth[0] += depth[0];
    		final double accumulatedDiscountFactor = Math.pow(discountFactor, depth[0]);
    		for (int j = 0; j < avgScores.length; j++) {
    			avgScores[j] += getGoal(stateForCharge, getRoles().get(j)) * accumulatedDiscountFactor;
    		}
    	}
    	avgDepth[0] /= repetitions;
    	for (int j = 0; j < avgScores.length; j++) {
    		avgScores[j] /= repetitions;
    	}
    }
}