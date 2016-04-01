package org.ggp.base.apps.logging;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.logging.LogSummaryGenerator;
import org.ggp.base.util.match.Match;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

class TestLogSummaryGenerator extends LogSummaryGenerator {
    @Override
    public String getSummaryFromLogsDirectory(String theLogsDirectory) {
        return "fake log for " + theLogsDirectory;
    }
}

class StuckTestLogSummaryGenerator extends LogSummaryGenerator {
    @Override
    public String getSummaryFromLogsDirectory(String theLogsDirectory) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "fake log for " + theLogsDirectory;
    }
}

public class LogSummarizerTest extends Assert {
    class LogSummarizerRunner extends Thread {
        LogSummarizer theSummarizer;
        LogSummaryGenerator theGenerator;

        public LogSummarizerRunner(LogSummaryGenerator theGenerator) {
            this.theGenerator = theGenerator;
        }

        public void stopAbruptly() throws IOException {
            theSummarizer.stopAbruptly();
        }

        @Override
        public void run() {
            theSummarizer = new LogSummarizer(theGenerator);
            try {
                theSummarizer.runSummarizer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Before
    public void setUp() {
        Match m = new Match("fake_match", 0, 0, 0, Game.createEphemeralGame("( (role fake_role) )"), null);
        GamerLogger.startFileLogging(m, "fake_role");
    }

    @Test
    public void testBasicResponse() throws IOException, InterruptedException {
        LogSummarizerRunner r = new LogSummarizerRunner(new TestLogSummaryGenerator());
        r.start();
        Thread.sleep(100);

        Socket s = new Socket("127.0.0.1", LogSummarizer.SERVER_PORT);
        HttpWriter.writeAsClient(s, "", "fake_match", "", null);
        assertEquals("fake log for logs/fake_match-fake_role", HttpReader.readAsClient(s));

        r.stopAbruptly();
        r.join();
    }

    @Test
    public void testStuckResponse() throws IOException, InterruptedException {
        LogSummarizerRunner r = new LogSummarizerRunner(new StuckTestLogSummaryGenerator());
        r.start();
        Thread.sleep(100);

        Socket s = new Socket("127.0.0.1", LogSummarizer.SERVER_PORT);
        HttpWriter.writeAsClient(s, "", "fake_match", "", null);

        try {
            HttpReader.readAsClient(s, 50);
        } catch (SocketTimeoutException ste) {
            ;
        }

        r.stopAbruptly();
        r.join();
    }
}