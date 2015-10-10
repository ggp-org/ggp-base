package org.ggp.base.server.event;

import java.io.Serializable;
import java.util.List;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;


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
