package org.ggp.base.player.gamer.statemachine.configurable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.ggp.base.apps.player.config.ConfigPanel;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GameAnalysisException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * ConfigurablePlayer is a player that's designed to be configured via a set of
 * parameters that can be adjusted without any code modifications. It presents
 * a nice user interface for setting these parameters, and stores them as JSON
 * when the user clicks "save" (and loads them automatically when new players
 * are created).
 * 
 * @author Sam Schreiber
 */
public final class ConfigurableGamer extends StateMachineGamer
{
	private ConfigurableConfigPanel configPanel = new ConfigurableConfigPanel();
	private ConfigurableDetailPanel detailPanel = new ConfigurableDetailPanel();
	private Random theRandom = new Random();
	
	private ConfigurableDetailPanel.AggregatingCounter statesExpanded;
	private ConfigurableDetailPanel.AggregatingCounter simulationsDone;
	private ConfigurableDetailPanel.FixedCounter expectedScore;
	
	public ConfigurableGamer() {
		configPanel = new ConfigurableConfigPanel();
		detailPanel = new ConfigurableDetailPanel();
		statesExpanded = detailPanel.new AggregatingCounter("States Expanded", false);
		simulationsDone = detailPanel.new AggregatingCounter("Simulations Done", false);
		expectedScore = detailPanel.new FixedCounter("Expected Score", true);
	}
	
	@Override
	public String getName() {
		return configPanel.getParameter("name", "Player #1");
	}
	
	/**
	 * Employs a configurable, parametrized algorithm that can be adjusted
	 * with knobs and parameters that can be tweaked without detailed knowledge
	 * of the code for the player.
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		detailPanel.beginAddingDataPoints();
		
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 2500;
		long finishSelectionForcedBy = finishBy - 1000;		
		long finishSelectionBy = finishSelectionForcedBy - 1000;
		
		SelectMoveThread selectMoveThread = new SelectMoveThread(finishSelectionBy);
		selectMoveThread.start();
		try {			
			selectMoveThread.join(finishSelectionForcedBy - System.currentTimeMillis());
		} catch (InterruptedException e) {
			;
		}
		selectMoveThread.interrupt();
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = selectMoveThread.getSelectedMove();
		boolean ranOutOfTime = false;
		if (selection == null) {
			ranOutOfTime = true;
			selection = moves.get(0);
		}

		long stop = System.currentTimeMillis();
		
		detailPanel.addObservation(getMatch().getMoveHistory().size(), selection, stop - start, ranOutOfTime); 

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}
	
	@Override
	public void analyze(Game g, long timeout) throws GameAnalysisException {
		// This gamer does no game analysis.
	}	
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		String metagameStrategy = configPanel.getParameter("metagameStrategy", "None");
		if (metagameStrategy.equals("Random Exploration")) {
			long finishBy = timeout - 2500;
			int[] depth = new int[1];
			while (finishBy > System.currentTimeMillis()) {				
				getStateMachine().performDepthCharge(getStateMachine().getInitialState(), depth);
				statesExpanded.increment(depth[0]);
			}
		}
	}	
	
	@Override
	public void stateMachineStop() {
		// This gamer does no special cleanup when the match ends normally.
	}
	
	@Override
	public void stateMachineAbort() {
		// This gamer does no special cleanup when the match ends abruptly.
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		StateMachine theMachine;
		String stateMachine = configPanel.getParameter("stateMachine", "Prover");
		if (stateMachine.equals("Prover")) {
			theMachine = new ProverStateMachine();
		} else {
			theMachine = new ProverStateMachine();
		}
		if (configPanel.getParameter("cacheStateMachine", false)) {
			theMachine = new CachedStateMachine(theMachine);
		}
		return theMachine;
	}

	@Override
	public ConfigPanel getConfigPanel() {
		return configPanel;
	}
	
	@Override
	public DetailPanel getDetailPanel() {
		return detailPanel;
	}
	
	class SelectMoveThread extends Thread {
		private String strategy;
		private Move selection;
		private long finishBy;
		
		public SelectMoveThread(long finishBy) {
			this.strategy = configPanel.getParameter("strategy", "Noop");
			this.finishBy = finishBy;
			this.selection = null;			
		}
		
        public void run() {
        	try {
	    		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
	
	    		if (strategy.equals("Noop")) {
	    			selection = new Move(GdlPool.getConstant("Noop"));
	    		} else if (strategy.equals("Legal")) {
	    			selection = moves.get(0);
	    		} else if (strategy.equals("Random")) {
	    			selection = moves.get(theRandom.nextInt(moves.size()));
	    		} else if (strategy.equals("Puzzle")) {
	    			selection = selectPuzzleMove(finishBy);
	    		} else if (strategy.equals("Minimax")) {
	    			selection = selectMinimaxMove(finishBy);
	    		} else if (strategy.equals("Heuristic")) {
	    			selection = selectHeuristicMove(finishBy);
	    		} else if (strategy.equals("Monte Carlo")) {
	    			selection = selectMonteCarloMove(finishBy);
	    		}
        	} catch (MoveDefinitionException e) {
        		throw new RuntimeException(e);
        	} catch (TransitionDefinitionException e) {
        		throw new RuntimeException(e);
			} catch (GoalDefinitionException e) {
				throw new RuntimeException(e);
			}
        }
        
        public Move getSelectedMove() {
        	return selection;
        }
	}
	
	/* ==================== Strategy-specific gaming approaches ======================== */
	
	// PUZZLE GAMER
	
	public Move selectPuzzleMove(long finishBy) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// SamplePuzzleGamer can only play single player games (puzzles).
		if (getStateMachine().getRoles().size() > 1) {
			return new Move(GdlPool.getConstant("OOPS"));
		}
		
		// From the current state, consider all of the legal moves, and the states that
		// they will move the game into, and of those states, pick the one in which we can
		// get the highest score.
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;
		
		for (Move move : moves) {
			MachineState stateAfterMove = getStateMachine().getNextState(getCurrentState(), Arrays.asList(new Move[]{move}));
			statesExpanded.increment(1);
			int bestScoreAfterMove = getPuzzleBestScore(stateAfterMove);
			if (bestScoreAfterMove > bestScoreSoFar) {
				bestScoreSoFar = bestScoreAfterMove;
				bestMoveSoFar = move;
				if (bestScoreSoFar == 100) {
					break;
				}
			}			
		}

		expectedScore.set(bestScoreSoFar);
		return bestMoveSoFar;
	}
	
	private int getPuzzleBestScore(MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, getRole());
		}
		
		int bestScoreSoFar = -1;
		
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for (Move move : moves) {
			MachineState gameStateAfterMove = getStateMachine().getNextState(state, Arrays.asList(new Move[]{move}));
			statesExpanded.increment(1);
			int bestScoreAfterMove = getPuzzleBestScore(gameStateAfterMove);
			bestScoreSoFar = Math.max(bestScoreSoFar, bestScoreAfterMove);
			if (bestScoreSoFar == 100) break;
		}
		
		return bestScoreSoFar;
	}
	
	// MINIMAX GAMER
	
	public Move selectMinimaxMove(long finishBy) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;
		
		for (Move move : moves) {
			int bestScoreAfterMove = minimaxScoreForMove(getCurrentState(), move);
			if (bestScoreAfterMove > bestScoreSoFar) {
				bestScoreSoFar = bestScoreAfterMove;
				bestMoveSoFar = move;
				if (bestScoreSoFar == 100) {
					break;
				}
			}			
		}
		
		expectedScore.set(bestScoreSoFar);
		return bestMoveSoFar;
	}
	
	private int minimaxScoreForMove(MachineState state, Move myMove)  throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		int worstScoreSoFar = 100;
		for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state, getRole(), myMove)) {
			int bestScoreSoFar = -1;
			MachineState stateAfterMove = getStateMachine().getNextState(state, jointMove);
			statesExpanded.increment(1);
			if (getStateMachine().isTerminal(stateAfterMove)) {
				bestScoreSoFar = getStateMachine().getGoal(stateAfterMove, getRole());
			} else {
				// Choose the move for us in the next state which maximizes our score				
				List<Move> moves = getStateMachine().getLegalMoves(stateAfterMove, getRole());
				for (Move myNextMove : moves) {
					int bestScoreAfterMove = minimaxScoreForMove(stateAfterMove, myNextMove);
					bestScoreSoFar = Math.max(bestScoreSoFar, bestScoreAfterMove);
					if (bestScoreSoFar == 100) break;
				}
			}
			// Choose the joint move for the opponents that minimizes our score
			worstScoreSoFar = Math.min(worstScoreSoFar, bestScoreSoFar);
			if (worstScoreSoFar == 0) break;
		}
		return worstScoreSoFar;
	}
	
	// HEURISTIC GAMER
	
	public Move selectHeuristicMove(long finishBy) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;
		
		for (Move move : moves) {
			int bestScoreAfterMove = heuristicScoreForMove(getCurrentState(), move, configPanel.getParameter("maxPlys", 5));
			if (bestScoreAfterMove > bestScoreSoFar) {
				bestScoreSoFar = bestScoreAfterMove;
				bestMoveSoFar = move;
				if (bestScoreSoFar == 100) {
					break;
				}
			}
		}
		
		expectedScore.set(bestScoreSoFar);		
		return bestMoveSoFar;
	}
	
	private int heuristicScoreForMove(MachineState state, Move myMove, int depth)  throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		int worstScoreSoFar = 100;
		for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state, getRole(), myMove)) {
			int bestScoreSoFar = -1;
			MachineState stateAfterMove = getStateMachine().getNextState(state, jointMove);
			statesExpanded.increment(1);
			if (getStateMachine().isTerminal(stateAfterMove)) {
				bestScoreSoFar = getStateMachine().getGoal(stateAfterMove, getRole());
			} else if (depth == 0) {				
				bestScoreSoFar = new MixtureHeuristic().evaluate(stateAfterMove);
			} else {
				// Choose the move for us in the next state which maximizes our score				
				List<Move> moves = getStateMachine().getLegalMoves(stateAfterMove, getRole());
				for (Move myNextMove : moves) {
					int bestScoreAfterMove = heuristicScoreForMove(stateAfterMove, myNextMove, depth-1);
					bestScoreSoFar = Math.max(bestScoreSoFar, bestScoreAfterMove);
					if (bestScoreSoFar == 100) break;
				}
			}
			// Choose the joint move for the opponents that minimizes our score
			worstScoreSoFar = Math.min(worstScoreSoFar, bestScoreSoFar);
			if (worstScoreSoFar == 0) break;
		}
		return worstScoreSoFar;
	}
	
	/**
	 * Heuristics are used to assign scores to non-terminal states. They are inherently
	 * approximations, based on things like having many pieces, having a good position
	 * on the board, having many available moves, having accumulated many points, having
	 * captured an opponent's pieces, having tie-breaking pieces, etc. Coming up with
	 * heuristics that can be used across a wide range of games, rather than just for one
	 * specific game, can be challenging. This interface defines heuristics as having an
	 * evaluate function, which takes a state and returns a number between 0 and 100
	 * representing an approximate score for the state.
	 */
	interface Heuristic {
		int evaluate(MachineState state) throws MoveDefinitionException;
	}
	
	/**
	 * MixtureHeuristic is a mixture of other heuristics. Each heuristic will likely
	 * have strong points and weak points, and so by mixing them together, the overall
	 * evaluation is hopefully more significant than any of its individual components.
	 */
	class MixtureHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			double focusWeight = configPanel.getParameter("heuristicFocus", 1);
			double mobilityWeight = configPanel.getParameter("heuristicMobility", 1);
			double opponentFocusWeight = configPanel.getParameter("heuristicOpponentFocus", 1);
			double opponentMobilityWeight = configPanel.getParameter("heuristicOpponentMobility", 1);
			double totalWeight = focusWeight + mobilityWeight + opponentFocusWeight + opponentMobilityWeight;			
			return (int)((focusWeight*new FocusHeuristic().evaluate(state) +
					       mobilityWeight*new MobilityHeuristic().evaluate(state) +
					       opponentFocusWeight*new OpponentFocusHeuristic().evaluate(state) +
					       opponentMobilityWeight*new OpponentMobilityHeuristic().evaluate(state))/totalWeight);
		}
	}
	
	/**
	 * Some common methods for composing move-based heuristics: a way to get the
	 * number of available moves for your player and your opponents, and two ways
	 * to convert counts into scores, one where the score increases as the count
	 * goes up, and one where the score descends as the count goes up.
	 */
	abstract class MoveBasedHeuristic {
		protected int myMoveCount(MachineState state) throws MoveDefinitionException {
			return getStateMachine().getLegalMoves(state, getRole()).size();
		}
		protected int theirMoveCount(MachineState state)  throws MoveDefinitionException {
			return getStateMachine().getLegalJointMoves(state).size() / getStateMachine().getLegalMoves(state, getRole()).size();
		}
		protected int getDescendingScore(int forCount) {
			return (int)(100 * Math.exp((1 - forCount)/5.0));
		}
		protected int getAscendingScore(int forCount) {
			return (int)(100 * Math.exp(-1/forCount));
		}
	}
	
	/**
	 * FocusHeuristic is a heuristic that measures the goodness of a particular state
	 * by how few moves are available, based on the theory that states with fewer moves
	 * are better because it's easier to search deeper when fewer moves are available.
	 */
	class FocusHeuristic extends MoveBasedHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return getDescendingScore(myMoveCount(state));
		}		
	}
	
	/**
	 * MobilityHeuristic is a heuristic that measures the goodness of a particular state
	 * by how many moves are available, based on the theory that states with more moves
	 * are better because they offer you more options.
	 */
	class MobilityHeuristic extends MoveBasedHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return getAscendingScore(myMoveCount(state));
		}
	}
	
	/**
	 * OpponentFocusHeuristic is a heuristic that measures the goodness of a particular
	 * state by how few moves are available to other players, based on the theory that
	 * fewer moves for other players is better because it's easier to search deeper when
	 * fewer moves are available, and that it's good to deny other players options.
	 */
	class OpponentFocusHeuristic extends MoveBasedHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return getDescendingScore(theirMoveCount(state));
		}		
	}
	
	/**
	 * MobilityHeuristic is a heuristic that measures the goodness of a particular state
	 * by how many moves are available to other players, based on the theory that states
	 * with more moves are better because the options will confuse the other players and
	 * make it more difficult for them to find the right path.
	 */
	class OpponentMobilityHeuristic extends MoveBasedHeuristic implements Heuristic {
		@Override
		public int evaluate(MachineState state) throws MoveDefinitionException {
			return getAscendingScore(theirMoveCount(state));
		}
	}
	
	// MONTE CARLO GAMER
	
	public Move selectMonteCarloMove(long finishBy) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		
	    long timeToExpect = System.currentTimeMillis() + 1000;
	    
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);
		if (moves.size() > 1) {		
    		double[] moveTotalPoints = new double[moves.size()];
    		int[] moveTotalAttempts = new int[moves.size()];
    		
    		// Perform depth charges for each candidate move, and keep track
    		// of the total score and total attempts accumulated for each move.
    		for (int i = 0; true; i = (i+1) % moves.size()) {
    		    if (System.currentTimeMillis() > finishBy)
    		        break;
    		    
    		    double theScore = performMonteCarloDepthChargeFromMove(getCurrentState(), moves.get(i));
    		    moveTotalPoints[i] += theScore;
    		    moveTotalAttempts[i] += 1;
    		    
    		    simulationsDone.increment(1);
    		    
    		    if (System.currentTimeMillis() > timeToExpect) {
    		    	double bestChildValueSoFar = -1;
    	    		for (int j = 0; j < moves.size(); j++) {
    	    			bestChildValueSoFar = Math.max(bestChildValueSoFar, (double)moveTotalPoints[i] / moveTotalAttempts[i]);
    	    		}
    			    expectedScore.set(bestChildValueSoFar);
    		    	timeToExpect = System.currentTimeMillis() + 1000;
    		    }
    		}
    
    		// Compute the expected score for each move.
    		double[] moveExpectedPoints = new double[moves.size()];
    		for (int i = 0; i < moves.size(); i++) {
    		    moveExpectedPoints[i] = (double)moveTotalPoints[i] / moveTotalAttempts[i];    		    
    		}

    		// Find the move with the best expected score.
    		int bestMove = 0;
    		double bestMoveScore = moveExpectedPoints[0];
    		for (int i = 1; i < moves.size(); i++) {
    		    if (moveExpectedPoints[i] > bestMoveScore) {
    		        bestMoveScore = moveExpectedPoints[i];
    		        bestMove = i;
    		    }
    		}
    		selection = moves.get(bestMove);
		}
		return selection;
	}
	
	private int[] depth = new int[1];
	double performMonteCarloDepthChargeFromMove(MachineState theState, Move myMove) {	    
	    StateMachine theMachine = getStateMachine();
	    try {
            MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(theState, getRole(), myMove), depth);
            statesExpanded.increment(depth[0]);
            return theMachine.getGoal(finalState, getRole()) * Math.pow(1-(configPanel.getParameter("mcDecayRate", 0)/100.0), depth[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
	}
}