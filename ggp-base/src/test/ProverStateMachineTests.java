package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlTerm;
import util.kif.KifReader;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.implementation.prover.ProverStateMachine;

public class ProverStateMachineTests extends Assert {

    protected final ProverStateMachine sm = new ProverStateMachine();
    protected final GdlConstant C1 = GdlPool.getConstant("1");
    protected final GdlConstant C2 = GdlPool.getConstant("2");
    protected final GdlConstant C3 = GdlPool.getConstant("3");
    protected final GdlConstant C50 = GdlPool.getConstant("50");
    protected final GdlConstant C100 = GdlPool.getConstant("100");

    @Test
    public void testProverOnTicTacToe() throws Exception {
        List<Gdl> ticTacToeDesc = KifReader.read("games/games/ticTacToe/ticTacToe.kif");
        sm.initialize(ticTacToeDesc);
        MachineState state = sm.getInitialState();
        assertFalse(sm.isTerminal(state));
        GdlConstant X_PLAYER = GdlPool.getConstant("xplayer");
        GdlConstant O_PLAYER = GdlPool.getConstant("oplayer");
        GdlProposition X_PLAYER_P = GdlPool.getProposition(X_PLAYER);
        GdlProposition O_PLAYER_P = GdlPool.getProposition(O_PLAYER);
        Role xRole = new Role(X_PLAYER_P);
        Role oRole = new Role(O_PLAYER_P);
        List<Role> roles = Arrays.asList(xRole, oRole);
        assertEquals(roles, sm.getRoles());

        assertEquals(9, sm.getLegalJointMoves(state).size());
        assertEquals(9, sm.getLegalMoves(state, xRole).size());
        assertEquals(1, sm.getLegalMoves(state, oRole).size());
        Move noop = new Move(GdlPool.getProposition(GdlPool.getConstant("noop")));
        assertEquals(noop, sm.getLegalMoves(state, oRole).get(0));

        Move m11 = move("mark 1 1");
        assertTrue(sm.getLegalMoves(state, xRole).contains(m11));
        state = sm.getNextState(state, Arrays.asList(new Move[] {m11, noop}));
        assertFalse(sm.isTerminal(state));

        Move m13 = move("mark 1 3");
        assertTrue(sm.getLegalMoves(state, oRole).contains(m13));
        state = sm.getNextState(state, Arrays.asList(new Move[] {noop, m13}));
        assertFalse(sm.isTerminal(state));

        Move m31 = move("mark 3 1");
        assertTrue(sm.getLegalMoves(state, xRole).contains(m31));
        state = sm.getNextState(state, Arrays.asList(new Move[] {m31, noop}));
        assertFalse(sm.isTerminal(state));

        Move m22 = move("mark 2 2");
        assertTrue(sm.getLegalMoves(state, oRole).contains(m22));
        state = sm.getNextState(state, Arrays.asList(new Move[] {noop, m22}));
        assertFalse(sm.isTerminal(state));

        Move m21 = move("mark 2 1");
        assertTrue(sm.getLegalMoves(state, xRole).contains(m21));
        state = sm.getNextState(state, Arrays.asList(new Move[] {m21, noop}));
        assertTrue(sm.isTerminal(state));
        assertEquals(100, sm.getGoal(state, xRole));
        assertEquals(0, sm.getGoal(state, oRole));
        assertEquals(Arrays.asList(new Integer[] {100, 0}), sm.getGoals(state));

        //My expectations for the behavior, but there's no consensus...
        /*Move m23 = new Move(GdlPool.getRelation(PLAY, new GdlTerm[] {C2, C3, O}));
        try {
            sm.getNextState(state, Arrays.asList(new Move[] {noop, m23}));
            fail("Should throw an exception when trying to transition from a terminal state");
        } catch(TransitionDefinitionException e) {
            //Expected
        }*/
    }

    @Test
    public void testCase1A() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_1a.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        assertFalse(sm.isTerminal(state));
        assertEquals(100, sm.getGoal(state, you));
        assertEquals(Collections.singletonList(100), sm.getGoals(state));
        state = sm.getNextState(state, Collections.singletonList(move("proceed")));
        assertTrue(sm.isTerminal(state));
        assertEquals(100, sm.getGoal(state, you));
        assertEquals(Collections.singletonList(100), sm.getGoals(state));
    }

    @Test
    public void testCase3C() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_3c.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role xplayer = new Role(GdlPool.getProposition(GdlPool.getConstant("xplayer")));
        assertFalse(sm.isTerminal(state));
        assertEquals(1, sm.getLegalMoves(state, xplayer).size());
        assertEquals(move("win"), sm.getLegalMoves(state, xplayer).get(0));
        state = sm.getNextState(state, Collections.singletonList(move("win")));
        assertTrue(sm.isTerminal(state));
        assertEquals(100, sm.getGoal(state, xplayer));
        assertEquals(Collections.singletonList(100), sm.getGoals(state));
    }

    @Test
    public void testCase5A() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_5a.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        assertFalse(sm.isTerminal(state));
        assertEquals(1, sm.getLegalMoves(state, you).size());
        assertEquals(move("proceed"), sm.getLegalMoves(state, you).get(0));
        state = sm.getNextState(state, Collections.singletonList(move("proceed")));
        assertTrue(sm.isTerminal(state));
        assertEquals(100, sm.getGoal(state, you));
        assertEquals(Collections.singletonList(100), sm.getGoals(state));
    }

    @Test
    public void testCase5B() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_5b.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        assertFalse(sm.isTerminal(state));
        assertEquals(1, sm.getLegalMoves(state, you).size());
        assertEquals(move("draw 1 1 1 2"), sm.getLegalMoves(state, you).get(0));
        state = sm.getNextState(state, Collections.singletonList(move("draw 1 1 1 2")));
        assertTrue(sm.isTerminal(state));
    }
    
    @Test
    public void testCase5C() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_5c.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        assertFalse(sm.isTerminal(state));
        assertEquals(1, sm.getLegalMoves(state, you).size());
        assertEquals(move("proceed"), sm.getLegalMoves(state, you).get(0));
        state = sm.getNextState(state, Collections.singletonList(move("proceed")));
        assertTrue(sm.isTerminal(state));
        assertEquals(100, sm.getGoal(state, you));
        assertEquals(Collections.singletonList(100), sm.getGoals(state));
    }
    
    protected Move move(String description) {
        String[] parts = description.split(" ");
        GdlConstant head = GdlPool.getConstant(parts[0]);
        if(parts.length == 1)
            return new Move(GdlPool.getProposition(head));
        List<GdlTerm> body = new ArrayList<GdlTerm>();
        for(int i = 1; i < parts.length; i++) {
            body.add(GdlPool.getConstant(parts[i]));
        }
        return new Move(GdlPool.getRelation(head, body));
    }
}
