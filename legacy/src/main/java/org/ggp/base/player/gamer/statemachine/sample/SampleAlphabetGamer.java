package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SampleAlphabetGamer is a minimal gamer which always plays the whichever
 * legal move comes first in alphabetical order, regardless of the state of
 * the game.
 *
 * For your first players, you should extend the class SampleGamer
 * The only function that you are required to override is :
 * public Move stateMachineSelectMove(long timeout)
 *
 */
public final class SampleAlphabetGamer extends SampleGamer
{
    /**
     * This function is called at the start of each round
     * You are required to return the Move your player will play
     * before the timeout.
     *
     */
    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
        // We get the current start time
        long start = System.currentTimeMillis();

        /**
         * We put in memory the list of legal moves from the
         * current state. The goal of every stateMachineSelectMove()
         * is to return one of these moves. Choosing the best
         * Move to play is the goal of GGP.
         */
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

        // Create a copy of the list of legal moves, so we can modify it.
        moves = new ArrayList<Move>(moves);

        // Sort the list of moves alphabetically.
        Collections.sort(moves, new Comparator<Move>(){
            @Override public int compare(Move m1, Move m2) {
                return m1.toString().compareTo(m2.toString());
            }
        });

        // Pick the move that comes first alphabetically.
        Move selection = moves.get(0);

        // Get the time when we finished.
        // It is mandatory that "stop" be less than "timeout".
        long stop = System.currentTimeMillis();

        /**
         * These are functions used by other parts of the GGP codebase
         * You shouldn't worry about them, just make sure that you have
         * moves, selection, stop and start defined in the same way as
         * this example, and copy-paste these two lines in your player
         */
        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
        return selection;
    }
}