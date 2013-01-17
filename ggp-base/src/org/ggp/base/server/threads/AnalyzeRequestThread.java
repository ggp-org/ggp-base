package org.ggp.base.server.threads;

import org.ggp.base.server.GameServer;
import org.ggp.base.server.request.RequestBuilder;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.Role;


public final class AnalyzeRequestThread extends RequestThread
{
	public AnalyzeRequestThread(GameServer gameServer, Match match, Role role, String host, int port, String playerName)
	{
		super(gameServer, role, host, port, playerName, match.getAnalysisClock() * 1000, RequestBuilder.getAnalyzeRequest(match.getGame().getRules(), match.getAnalysisClock(), match.getGdlScrambler()));
	}
	
	@Override
	protected void handleResponse(String response) {
		;
	}
}