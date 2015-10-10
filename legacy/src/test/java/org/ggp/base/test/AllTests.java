package org.ggp.base.test;

import org.ggp.base.apps.logging.LogSummarizerTest;
import org.ggp.base.apps.tiltyard.TiltyardRequestFarmTest;
import org.ggp.base.player.gamer.clojure.ClojureGamerTest;
import org.ggp.base.player.gamer.python.PythonGamerTest;
import org.ggp.base.util.crypto.BaseCryptographyTest;
import org.ggp.base.util.crypto.BaseHashingTest;
import org.ggp.base.util.crypto.CanonicalJSONTest;
import org.ggp.base.util.crypto.SignableJSONTest;
import org.ggp.base.util.game.GameParsingTest;
import org.ggp.base.util.gdl.model.DependencyGraphsTest;
import org.ggp.base.util.gdl.model.SimpleSentenceFormTest;
import org.ggp.base.util.gdl.scrambler.GdlRendererTest;
import org.ggp.base.util.gdl.scrambler.GdlScramblerTest;
import org.ggp.base.util.gdl.transforms.GdlCleanerTest;
import org.ggp.base.util.http.HttpTest;
import org.ggp.base.util.presence.InfoResponseTest;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachineTest;
import org.ggp.base.validator.StaticValidationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    BaseCryptographyTest.class,
    BaseHashingTest.class,
    CanonicalJSONTest.class,
    ClojureGamerTest.class,
    DependencyGraphsTest.class,
    GameParsingTest.class,
    GdlCleanerTest.class,
    GdlRendererTest.class,
    GdlScramblerTest.class,
    HttpTest.class,
    InfoResponseTest.class,
    LogSummarizerTest.class,
    NoTabsInRulesheetsTest.class,
    ProverStateMachineTest.class,
    PythonGamerTest.class,
    SignableJSONTest.class,
    SimpleSentenceFormTest.class,
    StaticValidationTest.class,
    TiltyardRequestFarmTest.class,
})
public class AllTests {

}
