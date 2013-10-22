package org.ggp.base.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({GdlCleanerTests.class,
                     NoTabsInRulesheetsTest.class,
                     ProverStateMachineTests.class,
                     StaticValidationTests.class,
                     GameParsingTests.class
                     })
public class AllTests {
    
}
