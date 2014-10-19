package org.ggp.base.player.gamer.clojure;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.exception.AbortingException;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
import org.ggp.base.player.gamer.exception.StoppingException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;

import clojure.lang.RT;
import clojure.lang.Var;

/**
 * ClojureGamer is a superclass that allows you to hook Clojure gamers into the
 * rest of the Java framework. In order to do this, do the following:
 *
 * 1) Create a subclass of ClojureGamer that overrides getClojureGamerFile() and
 *    getClojureGamerName() to indicate where the Clojure source code file is.
 *    This is the Java stub that refers to the real Clojure gamer class.
 *
 * 2) Create the Clojure source code file, in the /src_clj/ directory in the root
 *    directory for this project. Make sure that the stub points to this class,
 *    and that the Clojure class is a valid subclass of Gamer.
 *
 * For examples where this has already been done, see @ClojureLegalGamerStub,
 * which is implemented in Clojure and hook into the Java framework using the
 * ClojureGamer stub.
 *
 * @author Sam Schreiber
 */
public abstract class ClojureGamer extends Gamer
{
    Gamer theClojureGamer;

    protected abstract String getClojureGamerFile();
    protected abstract String getClojureGamerName();

    // Gamer stubs are lazily loaded because the Clojure interface takes
    // time to initialize, so we only want to load it when necessary, and
    // not for light-weight things like returning the player name.
    private void lazilyLoadGamerStub() {
    	if (theClojureGamer == null) {
	        try {
	            // Load the Clojure script -- as a side effect this initializes the runtime.
	            RT.loadResourceScript(getClojureGamerFile() + ".clj");

	            // Get a reference to the gamer-generating function.
	            Var gamerVar = RT.var("gamer_namespace", getClojureGamerName());

	            // Call it!
	            theClojureGamer = (Gamer)gamerVar.invoke();
	        } catch(Exception e) {
	            GamerLogger.logError("GamePlayer", "Caught exception in Clojure initialization:");
	            GamerLogger.logStackTrace("GamePlayer", e);
	        }
    	}
    }

    // The following methods are overriden as 'final' because they should not
    // be changed in subclasses of this class. Subclasses of this class should
    // only implement getClojureGamerFile() and getClojureGamerName(), and then
    // implement the real methods in the actual Clojure gamer. Essentially, any
    // subclass of this class is a Java-implementation stub for the actual real
    // Clojure implementation.

    @Override
    public final void preview(Game game, long timeout) throws GamePreviewException {
    	lazilyLoadGamerStub();
        try {
            theClojureGamer.preview(game, timeout);
        } catch(GamePreviewException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineMetaGame:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final void metaGame(long timeout) throws MetaGamingException {
    	lazilyLoadGamerStub();
        theClojureGamer.setMatch(getMatch());
        theClojureGamer.setRoleName(getRoleName());
        try {
            theClojureGamer.metaGame(timeout);
        } catch(MetaGamingException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineMetaGame:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final GdlTerm selectMove(long timeout) throws MoveSelectionException {
    	lazilyLoadGamerStub();
        theClojureGamer.setMatch(getMatch());
        theClojureGamer.setRoleName(getRoleName());
        try {
            return theClojureGamer.selectMove(timeout);
        } catch(MoveSelectionException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineSelectMove:");
            GamerLogger.logStackTrace("GamePlayer", e);
            return null;
        }
    }

    @Override
    public final void stop() {
    	lazilyLoadGamerStub();
        theClojureGamer.setMatch(getMatch());
        theClojureGamer.setRoleName(getRoleName());
        try {
            theClojureGamer.stop();
        } catch(StoppingException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineStop:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    @Override
    public final void abort() {
    	lazilyLoadGamerStub();
        theClojureGamer.setMatch(getMatch());
        theClojureGamer.setRoleName(getRoleName());
        try {
            theClojureGamer.abort();
        } catch(AbortingException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineAbort:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

   @Override
    public final String getName() {
	   return getClojureGamerName();
    }
}