package server;

import java.util.ArrayList;
import java.util.List;

import server.event.ServerCompletedMatchEvent;
import server.event.ServerNewGameStateEvent;
import server.event.ServerNewMatchEvent;
import server.event.ServerNewMovesEvent;
import server.event.ServerTimeEvent;
import server.threads.PlayRequestThread;
import server.threads.StartRequestThread;
import server.threads.StopRequestThread;
import util.gdl.grammar.GdlSentence;
import util.match.Match;
import util.observer.Event;
import util.observer.Observer;
import util.observer.Subject;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.implementation.prover.ProverMachineState;
import util.statemachine.implementation.prover.ProverStateMachine;

public final class GameServer extends Thread implements Subject
{
    private MachineState currentState;
    private final List<String> hosts;
    private final Match match;

    private final List<Observer> observers;
    private final List<Integer> ports;
    private final List<String>  playerNames;
    private List<Move> previousMoves;
    private List<String> history;

    private final StateMachine stateMachine;

    public GameServer(Match match, List<String> hosts, List<Integer> ports, List<String> playerNames)
    {
        this.match = match;
        this.hosts = hosts;
        this.ports = ports;
        this.playerNames = playerNames;

        stateMachine = new ProverStateMachine();
        stateMachine.initialize(match.getDescription());
        currentState = stateMachine.getInitialState();
        previousMoves = null;
        
        history = new ArrayList<String>();
        observers = new ArrayList<Observer>();
    }

    public void addObserver(Observer observer)
    {
        observers.add(observer);
    }

    public List<Integer> getGoals() throws GoalDefinitionException
    {
        List<Integer> goals = new ArrayList<Integer>();
        for (Role role : stateMachine.getRoles())
        {
            goals.add(stateMachine.getGoal(currentState, role));
        }

        return goals;
    }
    
    public StateMachine getStateMachine()
    {
        return stateMachine;        
    }

    public void notifyObservers(Event event)
    {
        for (Observer observer : observers)
        {
            observer.observe(event);
        }
    }

    @Override
    public void run()
    {
        try
        {
            notifyObservers(new ServerNewMatchEvent(stateMachine.getRoles()));

            notifyObservers(new ServerTimeEvent(match.getStartClock() * 1000));
            sendStartRequests();

            while (!stateMachine.isTerminal(currentState))
            {
                notifyObservers(new ServerNewGameStateEvent((ProverMachineState)currentState));
                notifyObservers(new ServerTimeEvent(match.getPlayClock() * 1000));
                previousMoves = sendPlayRequests();

                notifyObservers(new ServerNewMovesEvent(previousMoves));
                history.add(currentState.toXML());
                currentState = stateMachine.getNextState(currentState, previousMoves);
                List<GdlSentence> movesAsGDL = new ArrayList<GdlSentence>();
                for(Move m : previousMoves)
                    movesAsGDL.add(m.getContents());
                match.appendMoves(movesAsGDL);
            }
            notifyObservers(new ServerNewGameStateEvent((ProverMachineState)currentState));
            notifyObservers(new ServerCompletedMatchEvent(getGoals()));
            sendStopRequests(previousMoves);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private synchronized List<Move> sendPlayRequests() throws InterruptedException, MoveDefinitionException
    {
        List<PlayRequestThread> threads = new ArrayList<PlayRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++)
        {
            List<Move> legalMoves = stateMachine.getLegalMoves(currentState, stateMachine.getRoles().get(i));
            threads.add(new PlayRequestThread(this, match, previousMoves, legalMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
        }
        for (PlayRequestThread thread : threads)
        {
            thread.start();
        }

        List<Move> moves = new ArrayList<Move>();
        for (PlayRequestThread thread : threads)
        {
            thread.join();
            moves.add(thread.getMove());
        }

        return moves;
    }

    private synchronized void sendStartRequests() throws InterruptedException
    {
        List<StartRequestThread> threads = new ArrayList<StartRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++)
        {
            threads.add(new StartRequestThread(this, match, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
        }
        for (StartRequestThread thread : threads)
        {
            thread.start();
        }
        for (StartRequestThread thread : threads)
        {
            thread.join();
        }
    }

    private synchronized void sendStopRequests(List<Move> previousMoves) throws InterruptedException
    {
        List<StopRequestThread> threads = new ArrayList<StopRequestThread>(hosts.size());
        for (int i = 0; i < hosts.size(); i++)
        {
            threads.add(new StopRequestThread(this, match, previousMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
        }
        for (StopRequestThread thread : threads)
        {
            thread.start();
        }
        for (StopRequestThread thread : threads)
        {
            thread.join();
        }
    }
    
    public List<String> getHistory()
    {
        return history;
    }
    
    public String getGameXML()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) sb.append(history.get(i));
        return sb.toString();
    }    
}