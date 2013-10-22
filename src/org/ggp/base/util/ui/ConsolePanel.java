package org.ggp.base.util.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

/**
 * ConsolePanel implements a light-weight panel that shows all of the
 * messages being send to stdout and stderr. This can be useful in a graphical
 * application that also needs to alert the user about warnings that occur in
 * lower-level components, like the network communication stack. 
 */
@SuppressWarnings("serial")
public class ConsolePanel extends JPanel {
    public ConsolePanel() {
        super(new BorderLayout());
        
        // Create an output console.
        outputConsole = new JTextArea();
        outputConsole.setEditable(false);
        outputConsole.setForeground(new Color(125, 0, 0));        
        outputConsole.setText("(Console output will be displayed here.)\n\n");
        JScrollPane outputConsolePane = new JScrollPane(outputConsole);
        
        setBorder(new TitledBorder("Java Console:"));
        add(outputConsolePane, BorderLayout.CENTER);     
        validate();
        
        // Send the standard out and standard error streams
        // to this panel, instead.
        OutputStream out = new OutputStream() {
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));        
    }
    
    private final JTextArea outputConsole;
    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                outputConsole.append(text);
            }
        });
    }
}