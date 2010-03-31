package server.event;

import java.io.Serializable;
import java.util.List;

import util.observer.Event;
import util.statemachine.Move;

@SuppressWarnings("serial")
public final class ServerNewMovesEvent extends Event implements Serializable
{

	private final List<Move> moves;

	public ServerNewMovesEvent(List<Move> moves)
	{
		this.moves = moves;
	}

	public List<Move> getMoves()
	{
		return moves;
	}

}
