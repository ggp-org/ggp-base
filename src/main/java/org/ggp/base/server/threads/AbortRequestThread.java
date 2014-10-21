package org.ggp.base.server.threads;

import org.ggp.base.server.GameServer;
import org.ggp.base.server.request.RequestBuilder;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.Role;

public final class AbortRequestThread extends RequestThread
{
	public AbortRequestThread(GameServer gameServer, Match match, Role role, String host, int port, String playerName)
	{
		super(gameServer, role, host, port, playerName, 1000, RequestBuilder.getAbortRequest(match.getMatchId()));
	}

	@Override
	protected void handleResponse(String response) {
		;
	}
}