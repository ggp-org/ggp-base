package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ProverStateMachineTests.class,
                     StaticValidationTests.class
                     })
public class AllTests {
    
}
