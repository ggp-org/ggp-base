package org.ggp.base.util.logging;

import java.io.File;
import java.io.FilenameFilter;

public abstract class LogSummaryGenerator {
    public String getLogSummary(String matchId) {
        final String thePrefix = matchId;
        File logsDirectory = new File("logs");
        FilenameFilter logsFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(thePrefix);
            }
        };
        String[] theMatchingMatches = logsDirectory.list(logsFilter);
        if (theMatchingMatches.length > 1) {
            System.err.println("Log summary retrieval for " + matchId + " matched multiple matches.");
        } else if (theMatchingMatches.length == 0) {
            System.err.println("Log summary retrieval for " + matchId + " matched zero matches.");
        } else {
            return getSummaryFromLogsDirectory(logsDirectory + "/" + theMatchingMatches[0]);
        }
        return null;        
    }
    
    public abstract String getSummaryFromLogsDirectory(String theLogsDirectory);
}