package apps.kiosk.server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import server.event.ServerConnectionErrorEvent;
import server.event.ServerTimeoutEvent;
import server.request.RequestBuilder;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.match.Match;
import util.statemachine.Role;

public final class KioskStartRequestThread extends Thread
{

	private final KioskGameServer gameServer;
	private final String host;
	private final Match match;
	private final int port;
	private final Role role;
	private final String playerName;

	public KioskStartRequestThread(KioskGameServer gameServer, Match match, Role role, String host, int port, String playerName)
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
			Socket socket = new Socket(host, port);
			String request = RequestBuilder.getStartRequest(match.getMatchId(), role, match.getDescription(), match.getStartClock(), match.getPlayClock());

			HttpWriter.writeAsClient(socket, request, playerName);
			HttpReader.readAsClient(socket, match.getStartClock() * 1000);

			socket.close();
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
