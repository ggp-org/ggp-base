package server.threads;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import server.GameServer;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerTimeoutEvent;
import server.request.RequestBuilder;
import util.http.HttpRequest;
import util.match.Match;
import util.statemachine.Role;

public final class StartRequestThread extends Thread
{
	private final GameServer gameServer;
	private final String host;
	private final Match match;
	private final int port;
	private final Role role;
	private final String playerName;

	public StartRequestThread(GameServer gameServer, Match match, Role role, String host, int port, String playerName)
	{
		this.gameServer = gameServer;
		this.match = match;
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
			String request = RequestBuilder.getStartRequest(match.getMatchId(), role, match.getGame().getRules(), match.getStartClock(), match.getPlayClock());
			HttpRequest.issueRequest(host, port, playerName, request, match.getStartClock() * 1000);
		}
		catch (SocketTimeoutException e)
		{
			gameServer.notifyObservers(new ServerTimeoutEvent(role));
		}
		catch (UnknownHostException e)
		{
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
		}
		catch (IOException e)
		{
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
		}
	}
}