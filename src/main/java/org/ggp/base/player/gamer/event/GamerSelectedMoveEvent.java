package org.ggp.base.player.gamer.event;

import java.util.List;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;

public final class GamerSelectedMoveEvent extends Event
{
	private final List<Move> moves;
	private final Move selection;
	private final long time;

	public GamerSelectedMoveEvent(List<Move> moves, Move selection, long time) {
		this.moves = moves;
		this.selection = selection;
		this.time = time;
	}

	public List<Move> getMoves() {
		return moves;
	}

	public Move getSelection() {
		return selection;
	}

	public long getTime() {
		return time;
	}
}
