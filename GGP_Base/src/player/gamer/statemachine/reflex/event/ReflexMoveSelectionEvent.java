package player.gamer.statemachine.reflex.event;

import java.util.List;

import util.observer.Event;
import util.statemachine.Move;

public final class ReflexMoveSelectionEvent extends Event
{

	private final List<Move> moves;
	private final Move selection;
	private final long time;

	public ReflexMoveSelectionEvent(List<Move> moves, Move selection, long time)
	{
		this.moves = moves;
		this.selection = selection;
		this.time = time;
	}

	public List<Move> getMoves()
	{
		return moves;
	}

	public Move getSelection()
	{
		return selection;
	}

	public long getTime()
	{
		return time;
	}

}
