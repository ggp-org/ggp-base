package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import server.event.ServerCompletedMatchEvent;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerIllegalMoveEvent;
import server.event.ServerNewGameStateEvent;
import server.event.ServerNewMatchEvent;
import server.event.ServerNewMovesEvent;
import server.event.ServerTimeEvent;
import server.event.ServerTimeoutEvent;
import server.threads.PlayRequestThread;
import server.threads.StartRequestThread;
import server.threads.StopRequestThread;
import util.match.Match;
import util.match.MatchPublisher;
import util.observer.Event;
import util.observer.Observer;
import util.observer.Subject;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public final class GameServer extends Thread implements Subject
{
    private final Match match;
    private final StateMachine stateMachine;    
    private MachineState currentState;    

    private final List<String> hosts;    
    private final List<Integer> ports;
    private final List<String>  playerNames;
    private final Boolean[] playerGetsUnlimitedTime;    
    
    private final List<Observer> observers;        
    private List<Move> previousMoves;
    private List<String> history;
    
    private Map<Role,String> mostRecentErrors;
    
    private String spectatorServerURL;
    private boolean forceUsingEntireClock;
    
    public GameServer(Match match, List<String> hosts, List<Integer> ports, List<String> playerNames) {
        this.match = match;
        
        this.hosts = hosts;
        this.ports = ports;        
        this.playerNames = playerNames;
        
        playerGetsUnlimitedTime = new Boolean[hosts.size()];
        Arrays.fill(playerGetsUnlimitedTime, Boolean.FALSE);        

        stateMachine = new ProverStateMachine();
        stateMachine.initialize(match.getGame().getRules());
        currentState = stateMachine.getInitialState();
        previousMoves = null;
        
        mostRecentErrors = new HashMap<Role,String>();        
        
        match.appendState(currentState.getContents());
        
        history = new ArrayList<String>();
        observers = new ArrayList<Observer>();
        
        spectatorServerURL = null;
        forceUsingEntireClock = false;
    }
    
    public String startPublishingToSpectatorServer(String theURL) {
        spectatorServerURL = theURL;
        return publishWhenNecessary();
    }
    
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public List<Integer> getGoals() throws GoalDefinitionException {
        List<Integer> goals = new ArrayList<Integer>();
        for (Role role : stateMachine.getRoles()) {
            goals.add(stateMachine.getGoal(currentState, role));
        }

        return goals;
    }
    
    public StateMachine getStateMachine() {
        return stateMachine;        
    }

    public void notifyObservers(Event event) {
        for (Observer observer : observers) {
            observer.observe(event);
        }
        
        // Add error events to mostRecentErrors for recording.
        if (event instanceof ServerIllegalMoveEvent) {
            ServerIllegalMoveEvent sEvent = (ServerIllegalMoveEvent)event;
            mostRecentErrors.put(sEvent.getRole(), "IL " + sEvent.getMove());
        } else if (event instanceof ServerTimeoutEvent) {
            ServerTimeoutEvent sEvent = (ServerTimeoutEvent)event;
            mostRecentErrors.put(sEvent.getRole(), "TO");            
        } else if (event instanceof ServerConnectionErrorEvent) {
            ServerConnectionErrorEvent sEvent = (ServerConnectionErrorEvent)event;
            mostRecentErrors.put(sEvent.getRole(), "CE");
        }
    }
    
    // Should be called after each move, to collect all of the errors
    // caused by players and write them into the match description.
    private void appendErrorsToMatchDescription() {
        List<String> theErrors = new ArrayList<String>();
        for (int i = 0; i < stateMachine.getRoles().size(); i++) {
            Role r = stateMachine.getRoles().get(i);
            if (mostRecentErrors.containsKey(r)) {
                theErrors.add(mostRecentErrors.get(r));
            } else {
                theErrors.add("");
            }
        }
        match.appendErrors(theErrors);
        mostRecentErrors.clear();        
    }

    @Override
    public void run() {
        try {
            notifyObservers(new ServerNewMatchEvent(stateMachine.getRoles()));                        
            notifyObservers(new ServerTimeEvent(match.getStartClock() * 1000));
            sendStartRequests();
            appendErrorsToMatchDescription();

            while (!stateMachine.isTerminal(currentState)) {
                publishWhenNecessary();
                notifyObservers(new ServerNewGameStateEvent(currentState));
                notifyObservers(new ServerTimeEvent(match.getPlayClock() * 1000));
                previousMoves = sendPlayRequests();

                notifyObservers(new ServerNewMovesEvent(previousMoves));
                history.add(currentState.toXML());
                currentState = stateMachine.getNextState(currentState, previousMoves);
                
                match.appendMoves2(previousMoves);
                match.appendState(currentState.getContents());
                appendErrorsToMatchDescription();
            }
            match.markCompleted(stateMachine.getGoals(currentState));
            publishWhenNecessary();
            notifyObservers(new ServerNewGameStateEvent(currentState));
            notifyObservers(new ServerCompletedMatchEvent(getGoals()));
            sendStopRequests(previousMoves);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String publishWhenNecessary() {
        if (spectatorServerURL != null) {
            try {
                return MatchPublisher.publishToSpectatorServer(spectatorServerURL, match);
            } catch (IOException e) {
                e.printStackTrace();                
            }
        }
        return null;
    }

    private synchronized List<Move> sendPlayRequests() throws InterruptedException, MoveDefinitionException {
        List<PlayRequestThread> threads = new ArrayList<PlayRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
            List<Move> legalMoves = stateMachine.getLegalMoves(currentState, stateMachine.getRoles().get(i));
            threads.add(new PlayRequestThread(this, match, previousMoves, legalMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i), playerGetsUnlimitedTime[i]));
        }
        for (PlayRequestThread thread : threads) {
            thread.start();
        }
        
        if (forceUsingEntireClock) {
            Thread.sleep(match.getPlayClock() * 1000);
        }

        List<Move> moves = new ArrayList<Move>();
        for (PlayRequestThread thread : threads) {
            thread.join();
            moves.add(thread.getMove());
        }

        return moves;
    }

    private synchronized void sendStartRequests() throws InterruptedException {
        List<StartRequestThread> threads = new ArrayList<StartRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
            threads.add(new StartRequestThread(this, match, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
        }
        for (StartRequestThread thread : threads) {
            thread.start();
        }
        if (forceUsingEntireClock) {
            Thread.sleep(match.getStartClock() * 1000);
        }        
        for (StartRequestThread thread : threads) {
            thread.join();
        }
    }

    private synchronized void sendStopRequests(List<Move> previousMoves) throws InterruptedException {
        List<StopRequestThread> threads = new ArrayList<StopRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
            threads.add(new StopRequestThread(this, match, previousMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
        }
        for (StopRequestThread thread : threads) {
            thread.start();
        }
        for (StopRequestThread thread : threads) {
            thread.join();
        }
    }
    
    public List<String> getHistory() {
        return history;
    }
    
    public String getGameXML() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) sb.append(history.get(i));
        return sb.toString();
    }
    
    public void givePlayerUnlimitedTime(int i) {
        playerGetsUnlimitedTime[i] = true;
    }

    // Why would you want to force the game server to wait for the entire clock?
    // This can be used to rate-limit matches, so that you don't overload supporting
    // services like the repository server, spectator server, players, etc.
    public void setForceUsingEntireClock() {
        forceUsingEntireClock = true;
    }
    
    public Match getMatch() {
        return match;
    }
}