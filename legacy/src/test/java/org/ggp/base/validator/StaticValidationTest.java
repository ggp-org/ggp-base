package org.ggp.base.validator;

import org.ggp.base.util.game.TestGameRepository;
import org.junit.Test;

public class StaticValidationTest {

    @Test
    public void testConn4Validation() throws Exception {
        validate("connectFour");
    }
    @Test
    public void testSimpleMutexValidation() throws Exception {
        validate("simpleMutex");
    }
    @Test(expected=ValidatorException.class)
    public void test1AValidation() throws Exception {
        validate("test_case_1a");
    }
    @Test
    public void test1BValidation() throws Exception {
        validate("test_case_1b");
    }
    @Test
    public void test2AValidation() throws Exception {
        validate("test_case_2a");
    }
    @Test
    public void test2BValidation() throws Exception {
        validate("test_case_2b");
    }
    @Test
    public void test2CValidation() throws Exception {
        validate("test_case_2c");
    }
    @Test(expected=ValidatorException.class)
    public void test3AValidation() throws Exception {
        validate("test_case_3a");
    }
    @Test(expected=ValidatorException.class)
    public void test3BValidation() throws Exception {
        validate("test_case_3b");
    }
    @Test
    public void test3CValidation() throws Exception {
        validate("test_case_3c");
    }
    @Test
    public void test3DValidation() throws Exception {
        validate("test_case_3d");
    }
    @Test(expected=ValidatorException.class)
    public void test3EValidation() throws Exception {
        validate("test_case_3e");
    }
    @Test(expected=ValidatorException.class)
    public void test3FValidation() throws Exception {
        validate("test_case_3f");
    }
    @Test
    public void test4AValidation() throws Exception {
        validate("test_case_4a");
    }
    @Test
    public void test5AValidation() throws Exception {
        validate("test_case_5a");
    }
    @Test
    public void test5BValidation() throws Exception {
        validate("test_case_5b");
    }
    @Test
    public void test5CValidation() throws Exception {
        validate("test_case_5c");
    }
    @Test(expected=ValidatorException.class)
    public void testCleanNotDistinctValidation() throws Exception {
        validate("test_clean_not_distinct");
    }
    @Test(expected=ValidatorException.class)
    public void testFunctionAritiesDiffer() throws Exception {
        validate("test_invalid_function_arities_differ");
    }
    @Test(expected=ValidatorException.class)
    public void testSentenceAritiesDiffer() throws Exception {
        validate("test_invalid_sentence_arities_differ");
    }

    @Test
    public void testTicTacToeValidation() throws Exception {
        validate("ticTacToe");
    }

    protected void validate(String gameName) throws Exception {
        new StaticValidator().checkValidity(new TestGameRepository().getGame(gameName));
    }
}
