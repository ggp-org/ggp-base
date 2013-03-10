package org.ggp.base.player.gamer.statemachine.sample;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GameAnalysisException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * SampleGamer is a simplified version of the StateMachineGamer, dropping some
 * advanced functionality so the example gamers can be presented concisely.
 */
public abstract class SampleGamer extends StateMachineGamer
{
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
	
	@Override
	public void analyze(Game g, long timeout) throws GameAnalysisException {
		// Sample gamers do no game analysis.
	}	
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Sample gamers do no metagaming at the beginning of the match.
	}	
	
	@Override
	public void stateMachineStop() {
		// Sample gamers do no special cleanup when the match ends normally.
	}
	
	@Override
	public void stateMachineAbort() {
		// Sample gamers do no special cleanup when the match ends abruptly.
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}	

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}	
}