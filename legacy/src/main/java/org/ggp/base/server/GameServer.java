package org.ggp.base.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.server.event.ServerAbortedMatchEvent;
import org.ggp.base.server.event.ServerCompletedMatchEvent;
import org.ggp.base.server.event.ServerConnectionErrorEvent;
import org.ggp.base.server.event.ServerIllegalMoveEvent;
import org.ggp.base.server.event.ServerMatchUpdatedEvent;
import org.ggp.base.server.event.ServerNewGameStateEvent;
import org.ggp.base.server.event.ServerNewMatchEvent;
import org.ggp.base.server.event.ServerNewMovesEvent;
import org.ggp.base.server.event.ServerTimeEvent;
import org.ggp.base.server.event.ServerTimeoutEvent;
import org.ggp.base.server.threads.AbortRequestThread;
import org.ggp.base.server.threads.PlayRequestThread;
import org.ggp.base.server.threads.PreviewRequestThread;
import org.ggp.base.server.threads.RandomPlayRequestThread;
import org.ggp.base.server.threads.StartRequestThread;
import org.ggp.base.server.threads.StopRequestThread;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.match.MatchPublisher;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public final class GameServer extends Thread implements Subject
{
    private final Match match;
    private final StateMachine stateMachine;
    private MachineState currentState;

    private final List<String> hosts;
    private final List<Integer> ports;
    private final Boolean[] playerGetsUnlimitedTime;
    private final Boolean[] playerPlaysRandomly;

    private final List<Observer> observers;
    private List<Move> previousMoves;

    private Map<Role,String> mostRecentErrors;

    private String saveToFilename;
    private String spectatorServerURL;
    private String spectatorServerKey;
    private boolean forceUsingEntireClock;

    public GameServer(Match match, List<String> hosts, List<Integer> ports) {
        this.match = match;

        this.hosts = hosts;
        this.ports = ports;

        playerGetsUnlimitedTime = new Boolean[hosts.size()];
        Arrays.fill(playerGetsUnlimitedTime, Boolean.FALSE);

        playerPlaysRandomly = new Boolean[hosts.size()];
        Arrays.fill(playerPlaysRandomly, Boolean.FALSE);

        stateMachine = new ProverStateMachine();
        stateMachine.initialize(match.getGame().getRules());
        currentState = stateMachine.getInitialState();
        previousMoves = null;

        mostRecentErrors = new HashMap<Role,String>();

        match.appendState(currentState.getContents());

        observers = new ArrayList<Observer>();

        spectatorServerURL = null;
        forceUsingEntireClock = false;
    }

    public void startSavingToFilename(String theFilename) {
    	saveToFilename = theFilename;
    }

    public String startPublishingToSpectatorServer(String theURL) {
        spectatorServerURL = theURL;
        return publishWhenNecessary();
    }

    @Override
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

    @Override
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
        	if (match.getPreviewClock() >= 0) {
        		sendPreviewRequests();
        	}

            notifyObservers(new ServerNewMatchEvent(stateMachine.getRoles(), currentState));
            notifyObservers(new ServerTimeEvent(match.getStartClock() * 1000));
            sendStartRequests();
            appendErrorsToMatchDescription();

            while (!stateMachine.isTerminal(currentState)) {
                publishWhenNecessary();
                saveWhenNecessary();
                notifyObservers(new ServerNewGameStateEvent(currentState));
                notifyObservers(new ServerTimeEvent(match.getPlayClock() * 1000));
                notifyObservers(new ServerMatchUpdatedEvent(match, spectatorServerKey, saveToFilename));
                previousMoves = sendPlayRequests();

                notifyObservers(new ServerNewMovesEvent(previousMoves));
                currentState = stateMachine.getNextState(currentState, previousMoves);

                match.appendMoves2(previousMoves);
                match.appendState(currentState.getContents());
                appendErrorsToMatchDescription();

                if (match.isAborted()) {
                	return;
                }
            }
            match.markCompleted(stateMachine.getGoals(currentState));
            publishWhenNecessary();
            saveWhenNecessary();
            notifyObservers(new ServerNewGameStateEvent(currentState));
            notifyObservers(new ServerCompletedMatchEvent(getGoals()));
            notifyObservers(new ServerMatchUpdatedEvent(match, spectatorServerKey, saveToFilename));
            sendStopRequests(previousMoves);
        } catch (InterruptedException ie) {
        	if (match.isAborted()) {
        		return;
        	} else {
        		ie.printStackTrace();
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    public void abort() {
    	try {
    		match.markAborted();
    		sendAbortRequests();
    		saveWhenNecessary();
    		publishWhenNecessary();
    		notifyObservers(new ServerAbortedMatchEvent());
    		notifyObservers(new ServerMatchUpdatedEvent(match, spectatorServerKey, saveToFilename));
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private void saveWhenNecessary() {
    	if (saveToFilename == null) {
    		return;
    	}

    	try {
			File file = new File(saveToFilename);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(match.toJSON().toString());
			bw.close();
			fw.close();
    	} catch (IOException ie) {
    		ie.printStackTrace();
    	}
    }

    private String publishWhenNecessary() {
        if (spectatorServerURL == null) {
        	return null;
        }

    	int nAttempt = 0;
    	while (true) {
            try {
            	spectatorServerKey = MatchPublisher.publishToSpectatorServer(spectatorServerURL, match);
            	return spectatorServerKey;
            } catch (IOException e) {
            	if (nAttempt > 9) {
            		e.printStackTrace();
            		return null;
            	}
            }
    		nAttempt++;
    	}
    }

    public String getSpectatorServerKey() {
    	return spectatorServerKey;
    }

    private synchronized List<Move> sendPlayRequests() throws InterruptedException, MoveDefinitionException {
        List<PlayRequestThread> threads = new ArrayList<PlayRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
            List<Move> legalMoves = stateMachine.getLegalMoves(currentState, stateMachine.getRoles().get(i));
            if (playerPlaysRandomly[i]) {
            	threads.add(new RandomPlayRequestThread(match, legalMoves));
            } else {
                threads.add(new PlayRequestThread(this, match, previousMoves, legalMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), getPlayerNameFromMatchForRequest(i), playerGetsUnlimitedTime[i]));
            }
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

    private synchronized void sendPreviewRequests() throws InterruptedException {
        List<PreviewRequestThread> threads = new ArrayList<PreviewRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
        	if (!playerPlaysRandomly[i]) {
        		threads.add(new PreviewRequestThread(this, match, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), getPlayerNameFromMatchForRequest(i)));
        	}
        }
        for (PreviewRequestThread thread : threads) {
            thread.start();
        }
        if (forceUsingEntireClock) {
            Thread.sleep(match.getStartClock() * 1000);
        }
        for (PreviewRequestThread thread : threads) {
            thread.join();
        }
    }

    private synchronized void sendStartRequests() throws InterruptedException {
        List<StartRequestThread> threads = new ArrayList<StartRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
        	if (!playerPlaysRandomly[i]) {
        		threads.add(new StartRequestThread(this, match, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), getPlayerNameFromMatchForRequest(i)));
        	}
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
        	if (!playerPlaysRandomly[i]) {
        		threads.add(new StopRequestThread(this, match, previousMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), getPlayerNameFromMatchForRequest(i)));
        	}
        }
        for (StopRequestThread thread : threads) {
            thread.start();
        }
        for (StopRequestThread thread : threads) {
            thread.join();
        }
    }

    private void sendAbortRequests() throws InterruptedException {
        List<AbortRequestThread> threads = new ArrayList<AbortRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
        	if (!playerPlaysRandomly[i]) {
        		threads.add(new AbortRequestThread(this, match, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), getPlayerNameFromMatchForRequest(i)));
        	}
        }
        for (AbortRequestThread thread : threads) {
            thread.start();
        }
        for (AbortRequestThread thread : threads) {
            thread.join();
        }
        interrupt();
    }

    public void givePlayerUnlimitedTime(int i) {
        playerGetsUnlimitedTime[i] = true;
    }

    public void makePlayerPlayRandomly(int i) {
        playerPlaysRandomly[i] = true;
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

    private String getPlayerNameFromMatchForRequest(int i) {
    	if (match.getPlayerNamesFromHost() != null) {
    		return match.getPlayerNamesFromHost().get(i);
    	} else {
    		return "";
    	}
    }
}