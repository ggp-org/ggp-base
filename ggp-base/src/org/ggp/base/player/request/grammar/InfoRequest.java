package org.ggp.base.player.request.grammar;

import org.ggp.base.player.gamer.Gamer;

import external.JSON.JSONException;
import external.JSON.JSONObject;

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
		try {
			JSONObject info = new JSONObject();
			info.put("name", gamer.getName());
			info.put("status", (gamer.getMatch() == null) ? "available" : "busy");
		    return info.toString();
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	@Override
	public String toString()
	{
		return "ping";
	}
}