package player.gamer.python.stubs;
import player.gamer.python.PythonGamer;

/**
 * PythonLegalGamerStub is a stub that points to a version of @LegalGamer that
 * has been implemented in Python. This stub needs to exist so that the Python
 * code can interoperate with the rest of the Java framework (and applications
 * like Kiosk and Tiltyard as a result).
 * 
 * @author Sam
 */
public final class PythonLegalGamerStub extends PythonGamer
{
    protected String getPythonGamerModule() { return "legal_gamer"; }
    protected String getPythonGamerName() { return "LegalGamer"; }
}