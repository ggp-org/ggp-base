package player.request.grammar;

import player.gamer.Gamer;

public final class LogSummaryRequest extends Request
{
    private final Gamer theGamer;
	private final String matchId;

	public LogSummaryRequest(Gamer theGamer, String matchId)
	{
	    this.theGamer = theGamer;
		this.matchId = matchId;
	}
	
	@Override
	public String getMatchId() {
		return matchId;
	}

    @Override
	public String process(long receptionTime)
	{
	    return theGamer.getLogSummaryGenerator().getLogSummary(matchId);
	}

	@Override
	public String toString()
	{
		return "log";
	}
}