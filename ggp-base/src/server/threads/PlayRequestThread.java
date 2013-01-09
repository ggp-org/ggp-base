package server.threads;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import server.GameServer;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerIllegalMoveEvent;
import server.event.ServerTimeoutEvent;
import server.request.RequestBuilder;
import util.gdl.factory.GdlFactory;
import util.http.HttpRequest;
import util.match.Match;
import util.statemachine.Move;
import util.statemachine.Role;
import util.symbol.factory.exceptions.SymbolFormatException;

public class PlayRequestThread extends Thread
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
			String request = (previousMoves == null) ? RequestBuilder.getPlayRequest(match.getMatchId()) : RequestBuilder.getPlayRequest(match.getMatchId(), previousMoves);
			String response = HttpRequest.issueRequest(host, port, playerName, request, unlimitedTime ? -1 : (match.getPlayClock() * 1000 + 1000));

			move = gameServer.getStateMachine().getMoveFromTerm(GdlFactory.createTerm(response));
			if (!new HashSet<Move>(legalMoves).contains(move))
			{
				gameServer.notifyObservers(new ServerIllegalMoveEvent(role, move));
				move = legalMoves.get(new Random().nextInt(legalMoves.size()));
			}
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
		catch (SymbolFormatException e)
		{
			gameServer.notifyObservers(new ServerIllegalMoveEvent(role, move));
			move = legalMoves.get(0);
		}
	}
}