package org.ggp.base.player.gamer.statemachine.sample;

import org.ggp.base.player.gamer.statemachine.strategic.StrategicGamer;
import org.ggp.base.player.strategy.algorithm.MonteCarloTreeSearchAlgorithm;

/**
 * SampleMonteCarloGamer is a simple state-machine-based Gamer. It will use a
 * pure Monte Carlo approach towards picking moves, doing simulations and then
 * choosing the move that has the highest expected score. It should be slightly
 * more challenging than the RandomGamer, while still playing reasonably fast.
 * 
 * However, right now it isn't challenging at all. It's extremely mediocre, and
 * doesn't even block obvious one-move wins. This is partially due to the speed
 * of the default state machine (which is slow) and mostly due to the algorithm
 * assuming that the opponent plays completely randomly, which is inaccurate.
 * 
 * @author Sam Schreiber
 * @author marybel.archer
 */

public class SampleMonteCarloGamer extends StrategicGamer {

	public SampleMonteCarloGamer() {
		super.setSearchAlgorithm(new MonteCarloTreeSearchAlgorithm(this));
	}
}