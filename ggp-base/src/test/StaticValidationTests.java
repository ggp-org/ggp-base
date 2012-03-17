package test;

import java.util.List;

import org.junit.Test;

import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import validator.StaticValidator;
import validator.exception.StaticValidatorException;

public class StaticValidationTests {

    @Test
    public void testAsteroidsValidation() throws Exception {
        validate("../rulesheets/asteroids");
    }
    @Test
    public void testConn4Validation() throws Exception {
        validate("../rulesheets/conn4");
    }
    @Test
    public void testSimpleMutexValidation() throws Exception {
        validate("simpleMutex");
    }
    @Test
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
    @Test
    public void test3AValidation() throws Exception {
        validate("test_case_3a");
    }
    @Test(expected=StaticValidatorException.class)
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
    @Test(expected=StaticValidatorException.class)
    public void test3EValidation() throws Exception {
        validate("test_case_3e");
    }
    @Test(expected=StaticValidatorException.class)
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
    @Test(expected=StaticValidatorException.class)
    public void testCleanNotDistinctValidation() throws Exception {
        validate("test_clean_not_distinct");
    }
    
    @Test
    public void testTicTacToeValidation() throws Exception {
        validate("../rulesheets/tictactoe");
    }
    
    protected void validate(String gameName) throws Exception {
        List<Gdl> desc = KifReader.read("games/test/"+gameName+".kif");
        StaticValidator.validateDescription(desc);
    }
    
}
