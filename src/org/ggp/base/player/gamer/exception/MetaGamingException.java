package org.ggp.base.player.gamer.exception;

@SuppressWarnings("serial")
public final class MetaGamingException extends Exception
{
	public MetaGamingException(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString()
	{
		return "An unhandled exception occurred during metagaming: " + super.toString();
	}

}
