package org.ggp.base.player.gamer.statemachine.human.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;

import com.google.common.collect.ImmutableList;


public final class HumanNewMovesEvent extends Event
{
    private final ImmutableList<Move> moves;
    private final Move selection;

    private HumanNewMovesEvent(ImmutableList<Move> moves, Move selection) {
        this.moves = moves;
        this.selection = selection;
    }

    public static HumanNewMovesEvent create(List<Move> moves, Move selection) {
        List<Move> sortedMoves = new ArrayList<Move>(moves);
        Collections.sort(sortedMoves, new Comparator<Move>(){
            @Override
            public int compare(Move o1, Move o2) {
                return o1.toString().compareTo(o2.toString());
                }
            });
        return new HumanNewMovesEvent(ImmutableList.copyOf(sortedMoves), selection);
    }

    public List<Move> getMoves() {
        return moves;
    }

    public Move getSelection() {
        return selection;
    }
}
