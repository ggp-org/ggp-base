package org.ggp.base.player.request.grammar;

import org.ggp.base.player.gamer.Gamer;

public final class PingRequest extends Request
{
	private final Gamer gamer;

	public PingRequest(Gamer gamer)
	{
		this.gamer = gamer;
	}
	
	@Override
	public String getMatchId() {
		return null;
	}

	@Override
	public String process(long receptionTime)
	{
	    return (gamer.getMatch() == null) ? "available" : "busy";
	}

	@Override
	public String toString()
	{
		return "ping";
	}
}