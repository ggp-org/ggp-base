package org.ggp.base.util.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.match.Match;


/**
 * GamerLogger is a customized logger designed for long-running game players.
 * Logs are written to directories on a per-game basis. Each logfile represents
 * a single logical component of the game playing program, identified whenever
 * the logger is called.
 * 
 * TODO: More details about specific use examples.
 * 
 * @author Sam Schreiber
 */
public class GamerLogger {
    // Public Interface
    public static void emitToConsole(String s) {
        // TODO: fix this hack!
        if(!writeLogsToFile && !suppressLoggerOutput) {
            System.out.print(s);
        }
    }
    
    public static void stopFileLogging() {
        log("Logger", "Stopped logging to files at: " + new Date());
        log("Logger", "LOG SEALED");
        writeLogsToFile = false;
    }
    
    public static void setSpilloverLogfile(String spilloverFilename) {
    	spilloverLogfile = spilloverFilename;
    }
    
    public static void startFileLogging(Match m, String roleName) {
        writeLogsToFile = true;
        myDirectory = "logs/" + m.getMatchId() + "-" + roleName;
        
        new File(myDirectory).mkdirs();
        
        log("Logger", "Started logging to files at: " + new Date());
        log("Logger", "Game rules: " + m.getGame().getRules());
        log("Logger", "Start clock: " + m.getStartClock());
        log("Logger", "Play clock: " + m.getPlayClock());        
    }
    
    public static void setFileToDisplay(String toFile) {
        filesToDisplay.add(toFile);
    }
    
    public static void setMinimumLevelToDisplay(int nLevel) {
        minLevelToDisplay = nLevel;
    }
    
    public static void setSuppressLoggerOutput(boolean bSuppress) {
        suppressLoggerOutput = bSuppress;
    }

    public static final int LOG_LEVEL_DATA_DUMP = 0;
    public static final int LOG_LEVEL_ORDINARY = 3;
    public static final int LOG_LEVEL_IMPORTANT = 6;
    public static final int LOG_LEVEL_CRITICAL = 9;
    
    public static void logError(String toFile, String message) {
        logEntry(System.err, toFile, message, LOG_LEVEL_CRITICAL);
        if(writeLogsToFile) {
            logEntry(System.err, "Errors", "(in " + toFile + ") " + message, LOG_LEVEL_CRITICAL);
        }
    }
        
    public static void log(String toFile, String message) {
        log(toFile, message, LOG_LEVEL_ORDINARY);
    }
    
    public static void log(String toFile, String message, int nLevel) {
        logEntry(System.out, toFile, message, nLevel);
    }    
    
    public static void logStackTrace(String toFile, Exception ex) {
        StringWriter s = new StringWriter();
        ex.printStackTrace(new PrintWriter(s));
        logError(toFile, s.toString());
    }
    
    public static void logStackTrace(String toFile, Error ex) {
        StringWriter s = new StringWriter();
        ex.printStackTrace(new PrintWriter(s));
        logError(toFile, s.toString());
    }    
    
    // Private Implementation    
    private static boolean writeLogsToFile = false;
    
    private static final Random theRandom = new Random();
    private static final Set<String> filesToSkip = new HashSet<String>();
    private static final long maximumLogfileSize = 25 * 1024 * 1024;
    
    private static void logEntry(PrintStream ordinaryOutput, String toFile, String message, int logLevel) {
        if(suppressLoggerOutput)
            return;
        
        // When we're not writing to a particular directory, and we're not spilling over into
        // a general logfile, write directly to the standard output unless it is really unimportant.
        if(!writeLogsToFile && spilloverLogfile == null) {
            if (logLevel >= LOG_LEVEL_ORDINARY) {
                ordinaryOutput.println("[" + toFile + "] " + message);
            }
            return;
        }
        
        try {
            String logMessage = logFormat(logLevel, ordinaryOutput == System.err, message);
            
            // If we are also displaying this file, write it to the standard output.
            if(filesToDisplay.contains(toFile) || logLevel >= minLevelToDisplay) {
                ordinaryOutput.println("[" + toFile + "] " + message);
            }
            
            // When constructing filename, if we are not writing to a particular directory,
            // go directly to the spillover file if one exists.
            String myFilename = myDirectory + "/" + toFile;
            if(!writeLogsToFile && spilloverLogfile != null) {
            	myFilename = spilloverLogfile;
            }

            // Periodically check to make sure we're not writing TOO MUCH to this file.
            if(filesToSkip.size() != 0 && filesToSkip.contains(myFilename)) {
                return;
            }
            if(theRandom.nextInt(1000) == 0) {
                // Verify that the file is not too large.
                if(new File(myFilename).length() > maximumLogfileSize) {
                    System.err.println("Adding " + myFilename + " to filesToSkip.");
                    filesToSkip.add(myFilename);
                    logLevel = 9;
                    logMessage = logFormat(logLevel, ordinaryOutput == System.err, "File too long; stopping all writes to this file.");
                }
            }            
            
            // Finally, write the log message to the file.
            BufferedWriter out = new BufferedWriter(new FileWriter(myFilename, true));
            out.write(logMessage);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }   
    
    private static String logFormat(int logLevel, boolean isError, String message) {
        String logMessage = "LOG " + System.currentTimeMillis() + " [L" + logLevel + "]: " + (isError ? "<ERR> " : "") + message;            
        if(logMessage.charAt(logMessage.length() - 1) != '\n') {
            logMessage += '\n';     // All log lines must end with a newline.
        }
        return logMessage;
    }
    
    private static String myDirectory;   
    private static HashSet<String> filesToDisplay = new HashSet<String>();
    private static int minLevelToDisplay = Integer.MAX_VALUE;
    private static boolean suppressLoggerOutput;
    private static String spilloverLogfile;    
}