package org.ggp.base.player.strategy.algorithm;

import java.util.List;

import org.ggp.base.util.statemachine.Move;

public interface SearchAlgorithm {

	Move getBestMove(List<Move> moves, long finishByMillis);

}
