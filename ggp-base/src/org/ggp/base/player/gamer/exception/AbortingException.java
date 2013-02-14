package org.ggp.base.player.gamer.exception;

@SuppressWarnings("serial")
public final class AbortingException extends Exception
{
	public AbortingException(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString()
	{
		return "An unhandled exception ocurred during aborting: " + super.toString();
	}

}
