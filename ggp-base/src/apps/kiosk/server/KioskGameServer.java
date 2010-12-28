package apps.kiosk.server;

import java.util.ArrayList;
import java.util.List;

import server.event.ServerCompletedMatchEvent;
import server.event.ServerNewGameStateEvent;
import server.event.ServerNewMatchEvent;
import server.event.ServerNewMovesEvent;
import server.event.ServerTimeEvent;
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

public final class KioskGameServer extends Thread implements Subject
{
	private MachineState currentState;
	private final List<String> hosts;
	private final Match match;

	private final List<Observer> observers;
	private final List<Integer> ports;
	private final List<String>  playerNames;
	private List<Move> previousMoves;

	private final StateMachine stateMachine;

	private final int nHumanPlayer;
	
	public KioskGameServer(Match match, List<String> hosts, List<Integer> ports, List<String> playerNames, int nHumanPlayer)
	{
		this.match = match;
		this.hosts = hosts;
		this.ports = ports;
		this.playerNames = playerNames;
		
		this.nHumanPlayer = nHumanPlayer;

		stateMachine = new ProverStateMachine();
		stateMachine.initialize(match.getGame().getRules());
		currentState = stateMachine.getInitialState();
		previousMoves = null;
		
		match.appendState(currentState.getContents());

		observers = new ArrayList<Observer>();
	}

	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	private List<Integer> getGoals() throws GoalDefinitionException
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
				currentState = stateMachine.getNextState(currentState, previousMoves);
				List<GdlSentence> movesAsGDL = new ArrayList<GdlSentence>();
				for(Move m : previousMoves)
					movesAsGDL.add(m.getContents());
				match.appendMoves(movesAsGDL);
				match.appendState(currentState.getContents());
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
		List<KioskPlayRequestThread> threads = new ArrayList<KioskPlayRequestThread>(hosts.size());
		for (int i = 0; i < hosts.size(); i++)
		{
			List<Move> legalMoves = stateMachine.getLegalMoves(currentState, stateMachine.getRoles().get(i));
			threads.add(new KioskPlayRequestThread(this, match, previousMoves, legalMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i), i == nHumanPlayer));
		}
		for (KioskPlayRequestThread thread : threads)
		{
			thread.start();
		}

		List<Move> moves = new ArrayList<Move>();
		for (KioskPlayRequestThread thread : threads)
		{
			thread.join();
			moves.add(thread.getMove());
		}

		return moves;
	}

	private synchronized void sendStartRequests() throws InterruptedException
	{
		List<KioskStartRequestThread> threads = new ArrayList<KioskStartRequestThread>(hosts.size());
		for (int i = 0; i < hosts.size(); i++)
		{
			threads.add(new KioskStartRequestThread(this, match, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
		}
		for (KioskStartRequestThread thread : threads)
		{
			thread.start();
		}
		for (KioskStartRequestThread thread : threads)
		{
			thread.join();
		}
	}

	private synchronized void sendStopRequests(List<Move> previousMoves) throws InterruptedException
	{
		List<KioskStopRequestThread> threads = new ArrayList<KioskStopRequestThread>(hosts.size());
		for (int i = 0; i < hosts.size(); i++)
		{
			threads.add(new KioskStopRequestThread(this, match, previousMoves, stateMachine.getRoles().get(i), hosts.get(i), ports.get(i), playerNames.get(i)));
		}
		for (KioskStopRequestThread thread : threads)
		{
			thread.start();
		}
		for (KioskStopRequestThread thread : threads)
		{
			thread.join();
		}
	}

}