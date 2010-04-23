package apps.consoles;

import java.util.Scanner;
import org.python.util.PythonInterpreter;

/**
 * PythonConsole is a Jython-based app that lets you interact with a Python
 * console that has full access to all of the Java classes in the project.
 * This allows you to quickly experiment with the classes, without having to
 * write a full-blown Java program.
 * 
 * TODO: This isn't a great implementation of a Python console. It would be
 *       excellent if there were a way to hook Idle, or another full-featured
 *       Python console implementation, into this so that it could access the
 *       GGP Base classes and also provide features like code completion, and
 *       hitting 'UP' to get previous lines, and so on.
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
                    if(e2.toString().startsWith("SyntaxError: (\"mismatched input '<EOF>' expecting INDENT\",")) {
                        String inLine = in.nextLine();
                        while(inLine.length() > 0) {
                            line = line + '\n' + inLine;
                            inLine = in.nextLine();
                        }
                        try {
                            interpreter.exec(line);
                        } catch(Exception e3) {
                            System.err.println(e3);
                        }
                    } else {
                        System.err.println(e2);
                    }
                }
            }
        }
    }
}