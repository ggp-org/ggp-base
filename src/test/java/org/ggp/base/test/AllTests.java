package org.ggp.base.test;


import org.ggp.base.util.presence.InfoResponseTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	GameParsingTests.class,
	GdlCleanerTests.class,
	InfoResponseTest.class,
	NoTabsInRulesheetsTest.class,
	ProverStateMachineTests.class,
	SimpleSentenceFormTest.class,
	StaticValidationTests.class
                     })
public class AllTests {

}
