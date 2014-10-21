package org.ggp.base.server.threads;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.ggp.base.server.GameServer;
import org.ggp.base.server.event.ServerConnectionErrorEvent;
import org.ggp.base.server.event.ServerTimeoutEvent;
import org.ggp.base.util.http.HttpRequest;
import org.ggp.base.util.statemachine.Role;


/**
 * RequestThread is an abstract class that serves as a framework for all of the
 * requests that the match host sends to the players. Requests always have a
 * target identified by hostname and port, a role and player name associated
 * with that host, a timeout, a game server to report connection issues to,
 * et cetera. This framework does the usual setup and try-catch responding so
 * that the concrete RequestThread subclasses can focus on request-specific
 * business logic.
 *
 * @author schreib
 */
public abstract class RequestThread extends Thread
{
	private final GameServer gameServer;
	private final String host;
	private final int port;
	private final String playerName;
	private final int timeout;
	private final Role role;
	private final String request;

	public RequestThread(GameServer gameServer, Role role, String host, int port, String playerName, int timeout, String request)
	{
		this.gameServer = gameServer;
		this.role = role;
		this.host = host;
		this.port = port;
		this.playerName = playerName;
		this.timeout = timeout;
		this.request = request;
	}

	protected abstract void handleResponse(String response);

	@Override
	public void run()
	{
		try {
			String response = HttpRequest.issueRequest(host, port, playerName, request, timeout);
			handleResponse(response);
		} catch (SocketTimeoutException e) {
			gameServer.notifyObservers(new ServerTimeoutEvent(role));
		} catch (UnknownHostException e) {
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
		} catch (IOException e) {
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
		}
	}
}