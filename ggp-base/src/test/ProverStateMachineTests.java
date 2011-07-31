package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

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

public class ProverStateMachineTests {

    protected final ProverStateMachine sm = new ProverStateMachine();
    protected final GdlConstant C1 = GdlPool.getConstant("1");
    protected final GdlConstant C2 = GdlPool.getConstant("2");
    protected final GdlConstant C3 = GdlPool.getConstant("3");
    protected final GdlConstant C50 = GdlPool.getConstant("50");
    protected final GdlConstant C100 = GdlPool.getConstant("100");

    @Test
    public void testProverOnTicTacToe() throws Exception {
        List<Gdl> ticTacToeDesc = KifReader.read("games/rulesheets/tictactoe.kif");
        sm.initialize(ticTacToeDesc);
        MachineState state = sm.getInitialState();
        Assert.assertFalse(sm.isTerminal(state));
        GdlConstant X_PLAYER = GdlPool.getConstant("xPlayer");
        GdlConstant O_PLAYER = GdlPool.getConstant("oPlayer");
        GdlProposition X_PLAYER_P = GdlPool.getProposition(X_PLAYER);
        GdlProposition O_PLAYER_P = GdlPool.getProposition(O_PLAYER);
        Role xRole = new Role(X_PLAYER_P);
        Role oRole = new Role(O_PLAYER_P);
        List<Role> roles = Arrays.asList(xRole, oRole);
        Assert.assertEquals(sm.getRoles(), roles);

        Assert.assertEquals(sm.getLegalJointMoves(state).size(), 9);
        Assert.assertEquals(sm.getLegalMoves(state, xRole).size(), 9);
        Assert.assertEquals(sm.getLegalMoves(state, oRole).size(), 1);
        Move noop = new Move(GdlPool.getProposition(GdlPool.getConstant("noop")));
        Assert.assertEquals(sm.getLegalMoves(state, oRole).get(0), noop);

        Move m11 = move("play 1 1 x");
        Assert.assertTrue(sm.getLegalMoves(state, xRole).contains(m11));
        state = sm.getNextState(state, Arrays.asList(new Move[] {m11, noop}));
        Assert.assertFalse(sm.isTerminal(state));

        Move m13 = move("play 1 3 o");
        Assert.assertTrue(sm.getLegalMoves(state, oRole).contains(m13));
        state = sm.getNextState(state, Arrays.asList(new Move[] {noop, m13}));
        Assert.assertFalse(sm.isTerminal(state));

        Move m31 = move("play 3 1 x");
        Assert.assertTrue(sm.getLegalMoves(state, xRole).contains(m31));
        state = sm.getNextState(state, Arrays.asList(new Move[] {m31, noop}));
        Assert.assertFalse(sm.isTerminal(state));

        Move m22 = move("play 2 2 o");
        Assert.assertTrue(sm.getLegalMoves(state, oRole).contains(m22));
        state = sm.getNextState(state, Arrays.asList(new Move[] {noop, m22}));
        Assert.assertFalse(sm.isTerminal(state));

        Move m21 = move("play 2 1 x");
        Assert.assertTrue(sm.getLegalMoves(state, xRole).contains(m21));
        state = sm.getNextState(state, Arrays.asList(new Move[] {m21, noop}));
        Assert.assertTrue(sm.isTerminal(state));
        Assert.assertEquals(sm.getGoal(state, xRole), 100);
        Assert.assertEquals(sm.getGoal(state, oRole), 0);
        Assert.assertEquals(sm.getGoals(state), Arrays.asList(new Integer[] {100, 0}));

        //My expectations for the behavior, but there's no consensus...
        /*Move m23 = new Move(GdlPool.getRelation(PLAY, new GdlTerm[] {C2, C3, O}));
        try {
            sm.getNextState(state, Arrays.asList(new Move[] {noop, m23}));
            Assert.fail("Should throw an exception when trying to transition from a terminal state");
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
        Assert.assertFalse(sm.isTerminal(state));
        Assert.assertEquals(sm.getGoal(state, you), 100);
        Assert.assertEquals(sm.getGoals(state), Collections.singletonList(100));
        state = sm.getNextState(state, Collections.singletonList(move("proceed")));
        Assert.assertTrue(sm.isTerminal(state));
        Assert.assertEquals(sm.getGoal(state, you), 100);
        Assert.assertEquals(sm.getGoals(state), Collections.singletonList(100));
    }

    @Test
    public void testCase3C() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_3c.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role xplayer = new Role(GdlPool.getProposition(GdlPool.getConstant("xplayer")));
        Assert.assertFalse(sm.isTerminal(state));
        Assert.assertEquals(sm.getLegalMoves(state, xplayer).size(), 1);
        Assert.assertEquals(sm.getLegalMoves(state, xplayer).get(0), move("win"));
        state = sm.getNextState(state, Collections.singletonList(move("win")));
        Assert.assertTrue(sm.isTerminal(state));
        Assert.assertEquals(sm.getGoal(state, xplayer), 100);
        Assert.assertEquals(sm.getGoals(state), Collections.singletonList(100));
    }

    @Test
    public void testCase5A() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_5a.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        Assert.assertFalse(sm.isTerminal(state));
        Assert.assertEquals(sm.getLegalMoves(state, you).size(), 1);
        Assert.assertEquals(sm.getLegalMoves(state, you).get(0), move("proceed"));
        state = sm.getNextState(state, Collections.singletonList(move("proceed")));
        Assert.assertTrue(sm.isTerminal(state));
        Assert.assertEquals(sm.getGoal(state, you), 100);
        Assert.assertEquals(sm.getGoals(state), Collections.singletonList(100));
    }

    @Test
    public void testCase5B() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_5b.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        Assert.assertFalse(sm.isTerminal(state));
        Assert.assertEquals(1, sm.getLegalMoves(state, you).size());
        Assert.assertEquals(move("draw 1 1 1 2"), sm.getLegalMoves(state, you).get(0));
        state = sm.getNextState(state, Collections.singletonList(move("draw 1 1 1 2")));
        Assert.assertTrue(sm.isTerminal(state));
    }
    
    @Test
    public void testCase5C() throws Exception {
        List<Gdl> desc = KifReader.read("games/test/test_case_5c.kif");
        sm.initialize(desc);
        MachineState state = sm.getInitialState();
        Role you = new Role(GdlPool.getProposition(GdlPool.getConstant("you")));
        Assert.assertFalse(sm.isTerminal(state));
        Assert.assertEquals(sm.getLegalMoves(state, you).size(), 1);
        Assert.assertEquals(sm.getLegalMoves(state, you).get(0), move("proceed"));
        state = sm.getNextState(state, Collections.singletonList(move("proceed")));
        Assert.assertTrue(sm.isTerminal(state));
        Assert.assertEquals(sm.getGoal(state, you), 100);
        Assert.assertEquals(sm.getGoals(state), Collections.singletonList(100));
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
