package org.ggp.base.player.gamer.exception;

@SuppressWarnings("serial")
public final class MoveSelectionException extends Exception
{
	public MoveSelectionException(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString()
	{
		return "An unhandled exception ocurred during move selection: " + super.toString();
	}

}
