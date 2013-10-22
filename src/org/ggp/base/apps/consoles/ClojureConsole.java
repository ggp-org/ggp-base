package org.ggp.base.apps.consoles;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

/**
 * ClojureConsole is a Clojure-based app that lets you interact with a Clojure
 * console that has full access to all of the Java classes in the project.
 * This allows you to quickly experiment with the classes, without having to
 * write a full-blown Java program.
 * 
 * TODO: This could use some helper scripts, to allow it to quickly load game
 *       rulesheets and so on. Right now you have to manually load everything
 *       when you want to create a state machine that's initialized to a game,
 *       which is pretty bothersome.
 * 
 * @author Sam
 */
public class ClojureConsole {
    public static void main(String[] args) {
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
}