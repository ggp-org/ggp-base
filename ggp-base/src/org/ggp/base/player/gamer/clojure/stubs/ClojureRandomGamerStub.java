package org.ggp.base.player.gamer.clojure.stubs;
import org.ggp.base.player.gamer.clojure.ClojureGamer;

/**
 * ClojureLegalGamerStub is a stub that points to a version of @RandomGamer that
 * has been implemented in Clojure. This stub needs to exist so that the Clojure
 * code can interoperate with the rest of the Java framework (and applications
 * like Kiosk and Tiltyard as a result).
 * 
 * @author Sam
 */
public final class ClojureRandomGamerStub extends ClojureGamer
{
    protected String getClojureGamerFile() { return "random_gamer"; }
    protected String getClojureGamerName() { return "ClojureRandomGamer"; }
}