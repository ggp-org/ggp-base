package org.ggp.base.server.threads;

import org.ggp.base.server.GameServer;
import org.ggp.base.server.request.RequestBuilder;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.Role;


public final class PreviewRequestThread extends RequestThread
{
	public PreviewRequestThread(GameServer gameServer, Match match, Role role, String host, int port, String playerName)
	{
		super(gameServer, role, host, port, playerName, match.getPreviewClock() * 1000, RequestBuilder.getPreviewRequest(match.getGame().getRules(), match.getPreviewClock(), match.getGdlScrambler()));
	}

	@Override
	protected void handleResponse(String response) {
		;
	}
}