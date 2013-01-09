package server.threads;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import server.GameServer;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerTimeoutEvent;
import server.request.RequestBuilder;
import util.http.HttpRequest;
import util.match.Match;
import util.statemachine.Move;
import util.statemachine.Role;

public final class StopRequestThread extends Thread
{
	private final GameServer gameServer;
	private final String host;
	private final Match match;
	private final int port;
	private final List<Move> previousMoves;
	private final Role role;
	private final String playerName;

	public StopRequestThread(GameServer gameServer, Match match, List<Move> previousMoves, Role role, String host, int port, String playerName)
	{
		this.gameServer = gameServer;
		this.match = match;
		this.previousMoves = previousMoves;
		this.role = role;
		this.host = host;
		this.port = port;
		this.playerName = playerName;
	}

	@Override
	public void run()
	{
		try
		{
			String request = (previousMoves == null) ? RequestBuilder.getStopRequest(match.getMatchId()) : RequestBuilder.getStopRequest(match.getMatchId(), previousMoves);
			HttpRequest.issueRequest(host, port, playerName, request, match.getPlayClock() * 1000);
		}
		catch (SocketTimeoutException e)
		{
			gameServer.notifyObservers(new ServerTimeoutEvent(role));
		}
		catch (IOException e)
		{
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
		}
	}
}