package player.gamer.lisp;

import java.io.File;

import util.configuration.ProjectConfiguration;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlSentence;
import util.kif.KifReader;
import util.logging.GamerLogger;
import util.match.Match;

import clojure.lang.RT;
import clojure.lang.Var;
import clojure.lang.Symbol;

import player.gamer.Gamer;
import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import player.gamer.lisp.stubs.LispLegalGamerStub;

/**
 * LispGamer is a superclass that allows you to hook Lisp gamers into the
 * rest of the Java framework. In order to do this, do the following:
 *
 * ???
 *
 * For examples where this has already been done, see @LispLegalGamerStub and
 * @LispRandomGamerStub, which are both implemented in Lisp and hook into
 * the Java framework using the LispGamer stubs.
 * 
 * @author Sam
 */
public abstract class LispGamer extends Gamer
{
    Gamer theLispGamer;

    protected abstract String getLispGamerFile();
    protected abstract String getLispGamerName();    
    
    public LispGamer() {
        super();
        
        try {
            // Load the Clojure script -- as a side effect this initializes the runtime.
            RT.loadResourceScript(getLispGamerFile() + ".clj");

            // Get a reference to the gamer-generating function.
            Var gamerVar = RT.var("gamer_namespace", getLispGamerName());
     
            // Call it!
            theLispGamer = (Gamer)gamerVar.invoke();
        } catch(Exception e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Lisp initialization:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }
    
    // The following methods are overriden as 'final' because they should not
    // be changed in subclasses of this class. Subclasses of this class should
    // only implement getPythonGamerName() and getPythonGamerModule(), and then
    // implement the real methods in the actual Python gamer. Essentially, any
    // subclass of this class is a Java-implementation stub for the actual real
    // Python implementation.
    
    @Override
    public final void metaGame(long timeout) throws MetaGamingException {
        theLispGamer.setMatch(getMatch());
        theLispGamer.setRoleName(getRoleName());
        try {
            theLispGamer.metaGame(timeout);
        } catch(RuntimeException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Lisp stateMachineMetaGame:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }
    
    @Override
    public final GdlSentence selectMove(long timeout) throws MoveSelectionException {
        theLispGamer.setMatch(getMatch());
        theLispGamer.setRoleName(getRoleName());
        try {
            return theLispGamer.selectMove(timeout);
        } catch(RuntimeException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Lisp stateMachineSelectMove:");
            GamerLogger.logStackTrace("GamePlayer", e);
            return null;
        }
    }
    
    @Override
    public final String getName() {
        try {
            return theLispGamer.getName();
        } catch(RuntimeException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Lisp getName:");
            GamerLogger.logStackTrace("GamePlayer", e);
            return this.getClass().getSimpleName();
        }
    } 

    // A utility function to get a Clojure Lisp console.
    @SuppressWarnings("unused")
    private static void CreateLispConsole() {
        Symbol CLOJURE_MAIN = Symbol.intern("clojure.main");
        Var REQUIRE = RT.var("clojure.core", "require");
        Var MAIN = RT.var("clojure.main", "main");
        try {
            REQUIRE.invoke(CLOJURE_MAIN);
            MAIN.applyTo(RT.seq(new String[]{}));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    // TODO: This code should be put somewhere more general.
    public static void main(String args[]) {
        try {
            Gamer g = new LispLegalGamerStub();
            System.out.println(g.getName());

            File file = new File(ProjectConfiguration.gameRulesheetsPath + "tictactoe.kif");
            Match m = new Match("", 1000, 1000, KifReader.read(file.getAbsolutePath()));
            g.setMatch(m);
            g.setRoleName(GdlPool.getProposition(GdlPool.getConstant("xplayer")));
            g.metaGame(1000);
            System.out.println(g.selectMove(1000));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}