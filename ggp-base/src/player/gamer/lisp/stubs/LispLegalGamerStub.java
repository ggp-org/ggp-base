package player.gamer.lisp.stubs;
import player.gamer.lisp.LispGamer;

/**
 * LispLegalGamerStub is a stub that points to a version of @LegalGamer that
 * has been implemented in Lisp. This stub needs to exist so that the Lisp
 * code can interoperate with the rest of the Java framework (and applications
 * like Kiosk and Tiltyard as a result).
 * 
 * @author Sam
 */
public final class LispLegalGamerStub extends LispGamer
{
    protected String getLispGamerFile() { return "legal_gamer"; }
    protected String getLispGamerName() { return "LegalGamer"; }
}