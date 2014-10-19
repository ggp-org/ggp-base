package org.ggp.base.player.gamer.python;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.exception.AbortingException;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
import org.ggp.base.player.gamer.exception.StoppingException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;


/**
 * PythonGamer is a superclass that allows you to hook Python gamers into the
 * rest of the Java framework. In order to do this, do the following:
 *
 * 1) Create a subclass of PythonGamer that overrides getPythonGamerName() and
 *    getPythonGamerModule() to indicate where the Python source code file is.
 *    This is the Java stub that refers to the real Python gamer class.
 *
 * 2) Create the Python source code file, in the /src_py/ directory in the root
 *    directory for this project. Make sure that the stub points to this class,
 *    and that the Python class is a valid subclass of Gamer.
 *
 * For examples where this has already been done, see @PythonRandomGamerStub, which
 * is implemented in Python and hooks into the Java framework using the PythonGamer stub.
 *
 * @author Sam
 * @author evancox
 */
public abstract class PythonGamer extends Gamer
{
    Gamer thePythonGamer;

    protected abstract String getPythonGamerName();
    protected abstract String getPythonGamerModule();

    // Gamer stubs are lazily loaded because the Python interface takes
    // time to initialize, so we only want to load it when necessary, and
    // not for light-weight things like returning the player name.
    private void lazilyLoadGamerStub() {
    	if (thePythonGamer == null) {
	        try {
	            // Load in the Python gamer, using a Jython intepreter.
	            PythonInterpreter interpreter = new PythonInterpreter();
	            interpreter.exec("from " + getPythonGamerModule() + " import " + getPythonGamerName());
	            PyObject thePyClass = interpreter.get(getPythonGamerName());
	            PyObject PyGamerObject = thePyClass.__call__();
	            thePythonGamer = (Gamer)PyGamerObject.__tojava__(Gamer.class);
	        } catch(Exception e) {
	            GamerLogger.logError("GamePlayer", "Caught exception in Python initialization:");
	            GamerLogger.logStackTrace("GamePlayer", e);
	        }
    	}
    }

    // The following methods are overriden as 'final' because they should not
    // be changed in subclasses of this class. Subclasses of this class should
    // only implement getPythonGamerName() and getPythonGamerModule(), and then
    // implement the real methods in the actual Python gamer. Essentially, any
    // subclass of this class is a Java-implementation stub for the actual real
    // Python implementation.

    @Override
    public final void preview(Game game, long timeout) throws GamePreviewException {
    	lazilyLoadGamerStub();
        try {
            thePythonGamer.preview(game, timeout);
        } catch(GamePreviewException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Python stateMachinePreview:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final void metaGame(long timeout) throws MetaGamingException {
    	lazilyLoadGamerStub();
        thePythonGamer.setMatch(getMatch());
        thePythonGamer.setRoleName(getRoleName());
        try {
            thePythonGamer.metaGame(timeout);
        } catch(MetaGamingException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Python stateMachineMetaGame:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final GdlTerm selectMove(long timeout) throws MoveSelectionException {
    	lazilyLoadGamerStub();
        thePythonGamer.setMatch(getMatch());
        thePythonGamer.setRoleName(getRoleName());
        try {
            return thePythonGamer.selectMove(timeout);
        } catch(MoveSelectionException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Python stateMachineSelectMove:");
            GamerLogger.logStackTrace("GamePlayer", e);
            return null;
        }
    }

    @Override
    public final void stop() {
    	lazilyLoadGamerStub();
    	thePythonGamer.setMatch(getMatch());
        thePythonGamer.setRoleName(getRoleName());
        try {
            thePythonGamer.stop();
        } catch(StoppingException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Python stateMachineStop:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final void abort() {
    	lazilyLoadGamerStub();
    	thePythonGamer.setMatch(getMatch());
        thePythonGamer.setRoleName(getRoleName());
        try {
            thePythonGamer.abort();
        } catch(AbortingException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Python stateMachineAbort:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final String getName() {
    	return getPythonGamerName();
    }
}