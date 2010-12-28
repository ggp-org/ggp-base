package apps.tiltyard;

import java.util.ArrayList;
import java.util.List;

import player.gamer.event.GamerCompletedMatchEvent;
import player.gamer.event.GamerNewMatchEvent;
import server.GameServer;
import server.event.ServerCompletedMatchEvent;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerIllegalMoveEvent;
import server.event.ServerNewGameStateEvent;
import server.event.ServerTimeoutEvent;
import util.match.Match;
import util.observer.Event;
import util.observer.Observer;
import util.observer.Subject;

/**
 * TiltyardEvent encapsulates all of the information about a single
 * match being run. It is responsible for starting the match, handling
 * callbacks, and collecting relevant statistics to be pulled into the
 * TiltyardEventsPanel.
 * 
 * @author Sam Schreiber
 */
public class TiltyardEvent implements Observer, Subject {
    // Finalized information about the match type in general
    public final String gameName;
    public final int playClock, startClock;
    public final int numPlayers;
    public final Match theMatchModel;
    
    // Specific information about the latest match
    public int moveCount = 0;
    public int errorCount_Timeouts = 0;
    public int errorCount_IllegalMoves = 0;
    public int errorCount_ConnectionErrors = 0;
    public List<Integer> latestGoals = null;    
    public String theStatus = "Awaiting match...";
    
    public int old_moveCount = 0;
    public int old_errorCount_Timeouts = 0;
    public int old_errorCount_IllegalMoves = 0;
    public int old_errorCount_ConnectionErrors = 0;
    
    public TiltyardEvent(String gameName, Match theMatchModel, int numPlayers) {
        this.gameName = gameName;
        this.theMatchModel = theMatchModel;
        this.playClock = theMatchModel.getPlayClock();
        this.startClock = theMatchModel.getStartClock();        
        this.numPlayers = numPlayers;
        resetStats();
    }

    private void resetStats() {
        this.old_moveCount = this.moveCount;
        this.old_errorCount_Timeouts = this.errorCount_Timeouts;
        this.old_errorCount_IllegalMoves = this.errorCount_IllegalMoves;
        this.old_errorCount_ConnectionErrors = this.errorCount_ConnectionErrors;
        
        observers.clear();
        this.theStatus = "Awaiting match...";
        errorCount_Timeouts = 0;
        errorCount_IllegalMoves = 0;
        errorCount_ConnectionErrors = 0;
        moveCount = 0;
        latestGoals = null;	
    }

    public void runEvent(List<String> hosts, List<String> names, List<Integer> ports) {
        try {
            notifyObservers(new GamerNewMatchEvent(null, null));

            Match theMatch = new Match("Tiltyard." + gameName + "." + System.currentTimeMillis(), startClock, playClock, theMatchModel.getGame());
 
            GameServer gameServer = new GameServer(theMatch, hosts, ports, names);
            gameServer.addObserver(this);
            gameServer.start();

            theStatus = "Metagaming...";
            notifyObservers(new GamerCompletedMatchEvent());

            // Play the game!
            gameServer.join();

            // Cleanup!
            resetStats();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getGoalString() {
        if(latestGoals == null)
            return "";

        String theScores = "(";
        for(int i = 0; i < latestGoals.size(); i++) {
            if(i > 0)
                theScores += ", ";
            theScores += "" + latestGoals.get(i);  
        }
        theScores += ")";

        return theScores;
    }

    // Observation handling code (gets info from game)
    public void observe(Event event) {
        if(event instanceof ServerCompletedMatchEvent) {
            ServerCompletedMatchEvent scme = (ServerCompletedMatchEvent) event;
            latestGoals = scme.getGoals();
            theStatus = "Finished.";
            notifyObservers(new GamerCompletedMatchEvent());
        } else if(event instanceof ServerIllegalMoveEvent) {
            errorCount_IllegalMoves++;
            notifyObservers(new GamerCompletedMatchEvent());
        } else if(event instanceof ServerTimeoutEvent) {
            errorCount_Timeouts++;
            notifyObservers(new GamerCompletedMatchEvent());
        } else if(event instanceof ServerConnectionErrorEvent) {
            errorCount_ConnectionErrors++;
            notifyObservers(new GamerCompletedMatchEvent());
        } else if(event instanceof ServerNewGameStateEvent) {	    
            if(theStatus.equals("Metagaming..."))
                theStatus = "Playing...";
            else
                moveCount++;
            notifyObservers(new GamerCompletedMatchEvent());
        }
    }

    // Observer handling code (passes info along to panel)
    private final List<Observer> observers = new ArrayList<Observer>();
    public void addObserver(Observer observer)
    {
        observers.add(observer);
    }

    public void notifyObservers(Event event)
    {
        for (Observer observer : observers)
        {
            observer.observe(event);
        }
    }
}