package player.gamer.clojure;

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
import player.gamer.clojure.stubs.ClojureLegalGamerStub;
import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;

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
 * @author Sam
 */
public abstract class ClojureGamer extends Gamer
{
    Gamer theClojureGamer;

    protected abstract String getClojureGamerFile();
    protected abstract String getClojureGamerName();    
    
    public ClojureGamer() {
        super();
        
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
    
    // The following methods are overriden as 'final' because they should not
    // be changed in subclasses of this class. Subclasses of this class should
    // only implement getClojureGamerFile() and getClojureGamerName(), and then
    // implement the real methods in the actual Clojure gamer. Essentially, any
    // subclass of this class is a Java-implementation stub for the actual real
    // Clojure implementation.
    
    @Override
    public final void metaGame(long timeout) throws MetaGamingException {
        theClojureGamer.setMatch(getMatch());
        theClojureGamer.setRoleName(getRoleName());
        try {
            theClojureGamer.metaGame(timeout);
        } catch(RuntimeException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineMetaGame:");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }
    
    @Override
    public final GdlSentence selectMove(long timeout) throws MoveSelectionException {
        theClojureGamer.setMatch(getMatch());
        theClojureGamer.setRoleName(getRoleName());
        try {
            return theClojureGamer.selectMove(timeout);
        } catch(RuntimeException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure stateMachineSelectMove:");
            GamerLogger.logStackTrace("GamePlayer", e);
            return null;
        }
    }
    
    @Override
    public final String getName() {
        try {
            return theClojureGamer.getName();
        } catch(RuntimeException e) {
            GamerLogger.logError("GamePlayer", "Caught exception in Clojure getName:");
            GamerLogger.logStackTrace("GamePlayer", e);
            return this.getClass().getSimpleName();
        }
    } 

    // A utility function to get a Clojure console.
    @SuppressWarnings("unused")
    private static void CreateClojureConsole() {
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
            Gamer g = new ClojureLegalGamerStub();
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