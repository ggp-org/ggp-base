package apps.consoles;

import java.util.Scanner;
import org.python.util.PythonInterpreter;

/**
 * PythonConsole is a Jython-based app that lets you interact with a Python
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
public class PythonConsole {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        PythonInterpreter interpreter = new PythonInterpreter();
        while(true) {
            System.out.print(":> ");
            String line = in.nextLine();
            try {
                System.out.println(interpreter.eval(line));
            } catch(Exception e) {
                try {
                    interpreter.exec(line);
                } catch(Exception e2) {
                    System.err.println(e2);
                }
            }
        }
    }
}