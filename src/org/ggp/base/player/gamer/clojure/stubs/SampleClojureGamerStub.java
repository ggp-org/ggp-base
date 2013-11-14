package org.ggp.base.player.gamer.clojure.stubs;
import org.ggp.base.player.gamer.clojure.ClojureGamer;

/**
 * ClojureLegalGamerStub is a stub that points to a version of @RandomGamer that
 * has been implemented in Clojure. This stub needs to exist so that the Clojure
 * code can interoperate with the rest of the Java framework (and applications
 * like Kiosk and PlayerPanel as a result).
 *
 * @author Sam
 */
public final class SampleClojureGamerStub extends ClojureGamer
{
    @Override
	protected String getClojureGamerFile() { return "sample_gamer"; }
    @Override
	protected String getClojureGamerName() { return "SampleClojureGamer"; }
}