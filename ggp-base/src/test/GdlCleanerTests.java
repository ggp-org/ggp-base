package test;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import util.gdl.grammar.Gdl;
import util.gdl.transforms.GdlCleaner;
import util.kif.KifReader;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import validator.StaticValidator;

public class GdlCleanerTests {

    @Test
    public void testCleanNotDistinct() throws Exception {
        List<Gdl> description = KifReader.read("games/test/test_clean_not_distinct.kif");
        description = GdlCleaner.run(description);
        
        StaticValidator.validateDescription(description);
        
        StateMachine sm = new ProverStateMachine();
        sm.initialize(description);
        MachineState state = sm.getInitialState();
        Assert.assertEquals(1, sm.getRoles().size());
        Role player = sm.getRoles().get(0);
        Assert.assertEquals(1, sm.getLegalMoves(state, player).size());
        state = sm.getNextStates(state).get(0);
        Assert.assertTrue(sm.isTerminal(state));
        Assert.assertEquals(100, sm.getGoal(state, player));
    }
    
}
