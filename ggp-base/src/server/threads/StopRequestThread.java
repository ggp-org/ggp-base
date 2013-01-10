package server.threads;

import java.util.List;

import server.GameServer;
import server.request.RequestBuilder;
import util.match.Match;
import util.statemachine.Move;
import util.statemachine.Role;

public final class StopRequestThread extends RequestThread
{
	public StopRequestThread(GameServer gameServer, Match match, List<Move> previousMoves, Role role, String host, int port, String playerName)
	{
		super(gameServer, role, host, port, playerName, match.getPlayClock() * 1000, RequestBuilder.getStopRequest(match.getMatchId(), previousMoves, match.getGdlScrambler()));
	}

	@Override
	protected void handleResponse(String response) {
		;
	}
}