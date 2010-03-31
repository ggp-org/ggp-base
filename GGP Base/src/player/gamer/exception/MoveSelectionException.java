package player.gamer.exception;

@SuppressWarnings("serial")
public final class MoveSelectionException extends Exception
{

	@Override
	public String toString()
	{
		return "An unhandled exception ocurred during move selection.";
	}

}
