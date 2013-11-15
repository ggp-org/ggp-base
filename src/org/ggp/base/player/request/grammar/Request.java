package org.ggp.base.player.request.grammar;

public abstract class Request
{

	public abstract String process(long receptionTime);

	public abstract String getMatchId();

	@Override
	public abstract String toString();

}
