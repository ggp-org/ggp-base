/**
 * This is an example of how to use the log summarizer server to make your own
 * log summaries available. First, you create a "log summary generator" that
 * gets requests for particular match IDs and returns log summaries for those
 * matches. Then, you create a "LogSummarizer" object constructed with the log
 * summary generator object as a parameter, and call "runSummarizer" to start
 * the log summarizer server using your custom-written log summary generator.
 */

package org.ggp.base.apps.logging;

import org.ggp.base.util.logging.LogSummaryGenerator;

// This is the custom-written log summary generator. You should write one
// of these that produces meaningful log summaries for your player.
class ExampleLogSummaryGenerator extends LogSummaryGenerator {
    @Override
    public String getSummaryFromLogsDirectory(String theLogsDirectory) {
        return "example log for " + theLogsDirectory;
    }
}

// This starts the log summarizer server using the custom-written log summary
// generator written above.
public class ExampleLogSummarizer {
    public static void main(String[] args) {
        new LogSummarizer(new ExampleLogSummaryGenerator()).runSummarizer();
    }
}