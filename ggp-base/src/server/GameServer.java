package server;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import server.event.ServerCompletedMatchEvent;
import server.event.ServerNewGameStateEvent;
import server.event.ServerNewMatchEvent;
import server.event.ServerNewMovesEvent;
import server.event.ServerTimeEvent;
import server.threads.PlayRequestThread;
import server.threads.StartRequestThread;
import server.threads.StopRequestThread;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlSentence;
import util.kif.KifReader;
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
import util.symbol.factory.exceptions.SymbolFormatException;
import util.xhtml.GameStateRenderPanel;
import apps.viewer.*;

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
		history = new ArrayList<String>();
		stateMachine = new ProverStateMachine();
		stateMachine.initialize(match.getDescription());
		currentState = stateMachine.getInitialState();
		previousMoves = null;

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
	
	public static void runMatch(Match match, List<String> hostnames, List<Integer> portNumbers, List<String> playerNames)
	{
		GameServer server = new GameServer(match, hostnames, portNumbers, playerNames);
		server.run();
		try {
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server.getGameXML();
	}

	/**
	 * Main method for running 
	 * args[0] = name of the game (e.g. tic tac toe)
	 * args[1] = startclock
	 * args[2] = playclock
	 * args[3] = the number of players for the given game
	 * args[4] = file generated by play script
	 * @param args
	 * @throws GdlFormatException 
	 * @throws SymbolFormatException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws GoalDefinitionException 
	 */
	
	

	public static void main(String[] args) throws IOException, SymbolFormatException, GdlFormatException, InterruptedException, GoalDefinitionException
	{
		String tourneyName = args[0];
		String gamename = args[1];
		String rulesheet = "games/rulesheets/" + gamename + ".kif";
		List<Gdl> description = KifReader.read(rulesheet);
		int startClock = Integer.valueOf(args[2]); 
		int playClock = Integer.valueOf(args[3]);
		if ((args.length - 4) % 3 != 0)
		{
			System.err.println("Incorrect player/port/host config");
			System.exit(1);
		}
		List<String> hostNames = new ArrayList<String>();
		List<String> playerNames = new ArrayList<String>();
		List<Integer> portNumbers = new ArrayList<Integer>();
		String matchname = tourneyName + gamename;
		for (int i = 4; i < args.length; i+= 3)
		{
			String hostname = args[i];
			Integer portnumber = Integer.valueOf(args[i + 1]);
			String playerName = args[i + 2];
			hostNames.add(hostname);
			portNumbers.add(portnumber);
			playerNames.add(playerName);
			matchname += playerName;
		}
		Match match = new Match(matchname, startClock, playClock, description);
		GameServer server = new GameServer(match, hostNames, portNumbers, playerNames);
		server.run();
		server.join();
		File f = new File(tourneyName);
		if (!f.exists())
		{
			f.mkdir();
			f = new File(tourneyName + "/scores");
			f.createNewFile();
		}
		f = new File(tourneyName + "/" + matchname);
		if (f.exists()) f.delete();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write(server.getGameXML());
		bw.flush();
		bw.close();
		bw = new BufferedWriter(new FileWriter(tourneyName + "/scores"));
		List<Integer> goals = server.getGoals();
		String goalstr = "";
		String playerstr = "";
		for (int i = 0; i < goals.size(); i++)
		{
			Integer goal = server.getGoals().get(i);
			goalstr += Integer.toString(goal);
			playerstr += playerNames.get(i);
			if (i != goals.size() - 1)
			{
				playerstr += ",";
				goalstr += ",";
			}
		}
		bw.write(playerstr + "=" + goalstr);
		bw.flush();
		bw.close();
		
		
	}
}