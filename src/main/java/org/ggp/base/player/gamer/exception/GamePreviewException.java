package org.ggp.base.player.gamer.exception;

@SuppressWarnings("serial")
public final class GamePreviewException extends Exception
{
	public GamePreviewException(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString()
	{
		return "An unhandled exception occurred during game previewing!";
	}

}
