package org.ggp.base.player.request.grammar;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.presence.InfoResponse;

public final class InfoRequest extends Request
{
	private final Gamer gamer;

	public InfoRequest(Gamer gamer)
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
		InfoResponse info = new InfoResponse();
		info.setName(gamer.getName());
		info.setStatus(gamer.getMatch() == null ? "available" : "busy");
		info.setSpecies(gamer.getSpecies());
		return info.toSymbol().toString();
	}

	@Override
	public String toString()
	{
		return "info";
	}
}