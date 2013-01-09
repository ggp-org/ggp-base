package server.threads;

import server.GameServer;
import server.request.RequestBuilder;
import util.match.Match;
import util.statemachine.Role;

public final class StartRequestThread extends RequestThread
{
	public StartRequestThread(GameServer gameServer, Match match, Role role, String host, int port, String playerName)
	{
		super(gameServer, role, host, port, playerName, match.getStartClock() * 1000, RequestBuilder.getStartRequest(match.getMatchId(), role, match.getGame().getRules(), match.getStartClock(), match.getPlayClock()));
	}
	
	@Override
	protected void handleResponse(String response) {
		;
	}
}