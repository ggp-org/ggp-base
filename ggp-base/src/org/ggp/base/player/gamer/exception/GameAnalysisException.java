package org.ggp.base.player.gamer.exception;

@SuppressWarnings("serial")
public final class GameAnalysisException extends Exception
{
	public GameAnalysisException(Throwable cause) {
		super(cause);
	}
	
	@Override
	public String toString()
	{
		return "An unhandled exception occurred during game analysis!";
	}

}
