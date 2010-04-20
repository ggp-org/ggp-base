package player.gamer.python.stubs;
import player.gamer.python.PythonGamer;

/**
 * PythonRandomGamerStub is a stub pointing to a version of @RandomGamer that
 * has been implemented in Python. This stub needs to exist so that the Python
 * code can interoperate with the rest of the Java framework (and applications
 * like Kiosk and Tiltyard as a result).
 * 
 * @author Sam
 */
public final class PythonRandomGamerStub extends PythonGamer
{
    protected String getPythonGamerModule() { return "random_gamer"; }
    protected String getPythonGamerName() { return "RandomGamer"; }
}