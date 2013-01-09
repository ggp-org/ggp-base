package server.threads;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import server.GameServer;
import server.event.ServerIllegalMoveEvent;
import server.request.RequestBuilder;
import util.gdl.factory.GdlFactory;
import util.match.Match;
import util.statemachine.Move;
import util.statemachine.Role;
import util.symbol.factory.exceptions.SymbolFormatException;

public class PlayRequestThread extends RequestThread
{
	private final GameServer gameServer;
	private final List<Move> legalMoves;
	private final Role role;
	
	private Move move;

	public PlayRequestThread(GameServer gameServer, Match match, List<Move> previousMoves, List<Move> legalMoves, Role role, String host, int port, String playerName, boolean unlimitedTime)
	{
		super(gameServer, role, host, port, playerName, unlimitedTime ? -1 : (match.getPlayClock() * 1000 + 1000), RequestBuilder.getPlayRequest(match.getMatchId(), previousMoves));
		this.gameServer = gameServer;
		this.legalMoves = legalMoves;
		this.role = role;

		move = legalMoves.get(new Random().nextInt(legalMoves.size()));
	}
	
	public Move getMove()
	{
		return move;
	}
	
	@Override
	protected void handleResponse(String response) {
		try {
			Move candidateMove = gameServer.getStateMachine().getMoveFromTerm(GdlFactory.createTerm(response));
			if (new HashSet<Move>(legalMoves).contains(candidateMove)) {
				move = candidateMove;
			} else {
				gameServer.notifyObservers(new ServerIllegalMoveEvent(role, candidateMove));
			}
		} catch (SymbolFormatException e) {
			gameServer.notifyObservers(new ServerIllegalMoveEvent(role, null));
		}

	}
}