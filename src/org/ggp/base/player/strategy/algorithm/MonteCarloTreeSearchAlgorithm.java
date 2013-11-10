package org.ggp.base.player.strategy.algorithm;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonteCarloTreeSearchAlgorithm implements SearchAlgorithm {
	private static final Logger logger = LoggerFactory.getLogger(MonteCarloTreeSearchAlgorithm.class);
	private int[] depth = new int[1];
	private StateMachineGamer gamer;

	public MonteCarloTreeSearchAlgorithm(StateMachineGamer gamer) {
		this.gamer = gamer;

	}

	@Override
	public Move getBestMove(List<Move> moves, long finishByMillis) {

		if (moves.size() > 1) {
			double[] moveExpectedPoints = getExpectedPointsForMoves(moves, finishByMillis);
			int bestMoveIndex = getBestMoveIndex(moves, moveExpectedPoints);

			return moves.get(bestMoveIndex);
		}

		return moves.get(0);
	}

	/**
	 * Perform depth charges for each candidate move, and keep track of the
	 * total score and total attempts accumulated for each move.
	 * 
	 * @param moves
	 * @param finishByMillis
	 * 
	 * @return
	 */
	private double[] getExpectedPointsForMoves(List<Move> moves,
			long finishByMillis) {

		int[] moveTotalPoints = new int[moves.size()];
		int[] moveTotalAttempts = new int[moves.size()];

		for (int i = 0; true; i = (i + 1) % moves.size()) {
			if (System.currentTimeMillis() > finishByMillis)
				break;

			int theScore = performDepthChargeFromMove(gamer.getCurrentState(), moves.get(i));
			moveTotalPoints[i] += theScore;
			moveTotalAttempts[i] += 1;
		}

		double[] moveExpectedPoints = calculateMoveExpectedPoints(moves, moveTotalPoints, moveTotalAttempts);
		logger.debug("moves = {}", moves);
		logger.debug("moveTotalPoints = {}", moveTotalPoints);
		logger.debug("moveTotalAttempts = {}", moveTotalAttempts);
		logger.debug("moveExpectedPoints = {}", moveExpectedPoints);
		return moveExpectedPoints;
	}

	private int performDepthChargeFromMove(MachineState theState,
			Move myMove) {

		try {
			MachineState randomNextState = gamer.getStateMachine()
					.getRandomNextState(theState, gamer.getRole(), myMove);
			MachineState finalState = gamer.getStateMachine().performDepthCharge(randomNextState, depth);

			return gamer.getStateMachine().getGoal(finalState, gamer.getRole());
		} catch (Exception e) {
			e.printStackTrace();

			return 0;
		}
	}

	private int getBestMoveIndex(List<Move> moves, double[] moveExpectedPoints) {
		int bestMove = 0;
		double bestMoveScore = moveExpectedPoints[0];
		for (int i = 1; i < moves.size(); i++) {
			if (moveExpectedPoints[i] > bestMoveScore) {
				bestMoveScore = moveExpectedPoints[i];
				bestMove = i;
			}
		}
		return bestMove;
	}

	private double[] calculateMoveExpectedPoints(List<Move> moves, int[] moveTotalPoints, int[] moveTotalAttempts) {
		double[] moveExpectedPoints = new double[moves.size()];
		for (int i = 0; i < moves.size(); i++) {
			moveExpectedPoints[i] = (double) moveTotalPoints[i] / moveTotalAttempts[i];
		}

		return moveExpectedPoints;
	}
}
