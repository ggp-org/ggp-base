package org.ggp.base.player.gamer.exception;

@SuppressWarnings("serial")
public final class StoppingException extends Exception
{
	public StoppingException(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString()
	{
		return "An unhandled exception ocurred during stopping: " + super.toString();
	}

}
