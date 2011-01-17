package server.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;

import server.GameServer;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerIllegalMoveEvent;
import server.event.ServerTimeoutEvent;
import server.request.RequestBuilder;
import util.gdl.factory.GdlFactory;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.GdlSentence;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.match.Match;
import util.statemachine.Move;
import util.statemachine.Role;
import util.symbol.factory.exceptions.SymbolFormatException;

public final class PlayRequestThread extends Thread
{
	private final GameServer gameServer;
	private final String host;
	private final List<Move> legalMoves;
	private final Match match;
	private final int port;
	private final String playerName;
	private final List<Move> previousMoves;
	private final boolean unlimitedTime;
	private final Role role;
	
	private Move move;

	public PlayRequestThread(GameServer gameServer, Match match, List<Move> previousMoves, List<Move> legalMoves, Role role, String host, int port, String playerName, boolean unlimitedTime)
	{
		this.gameServer = gameServer;
		this.match = match;
		this.previousMoves = previousMoves;
		this.legalMoves = legalMoves;
		this.role = role;
		this.host = host;
		this.port = port;
		this.playerName = playerName;
		this.unlimitedTime = unlimitedTime;

		move = null;
	}

	public Move getMove()
	{
		return move;
	}

	@Override
	public void run()
	{
		try
		{
		    InetAddress theHost = InetAddress.getByName(host);
		    
			Socket socket = new Socket(theHost.getHostAddress(), port);
			String request = (previousMoves == null) ? RequestBuilder.getPlayRequest(match.getMatchId()) : RequestBuilder.getPlayRequest(match.getMatchId(), previousMoves);

			HttpWriter.writeAsClient(socket, theHost.getHostName(), request, playerName);
			String response = unlimitedTime ? HttpReader.readAsClient(socket) : HttpReader.readAsClient(socket, match.getPlayClock() * 1000 + 1000);

			move = gameServer.getStateMachine().getMoveFromSentence((GdlSentence) GdlFactory.create(response));
			if (!new HashSet<Move>(legalMoves).contains(move))
			{
				gameServer.notifyObservers(new ServerIllegalMoveEvent(role, move));
				move = legalMoves.get(0);
			}

			socket.close();
		}
		catch (SocketTimeoutException e)
		{
			gameServer.notifyObservers(new ServerTimeoutEvent(role));
			move = legalMoves.get(0);
		}
		catch (IOException e)
		{
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
			move = legalMoves.get(0);
		}
		catch (GdlFormatException e)
		{
			gameServer.notifyObservers(new ServerIllegalMoveEvent(role, move));
			move = legalMoves.get(0);
		}
		catch (SymbolFormatException e)
		{
			gameServer.notifyObservers(new ServerIllegalMoveEvent(role, move));
			move = legalMoves.get(0);
		}
	}
}