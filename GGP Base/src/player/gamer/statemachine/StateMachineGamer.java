package player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;

import player.gamer.Gamer;
import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import util.gdl.grammar.GdlSentence;
import util.logging.GamerLogger;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public abstract class StateMachineGamer extends Gamer
{
	private MachineState currentState;
	private Role role;
	private StateMachine stateMachine;

	public final MachineState getCurrentState()
	{
		return currentState;
	}

	public final Role getRole()
	{
		return role;
	}

	public final StateMachine getStateMachine()
	{
		return stateMachine;
	}
	
	@Override
	public final void metaGame(long timeout) throws MetaGamingException
	{
		try
		{
			stateMachine = getInitialStateMachine();
			stateMachine.initialize(getMatch().getDescription());
			currentState = stateMachine.getInitialState();
			role = stateMachine.getRoleFromProp(getRoleName());

			stateMachineMetaGame(timeout);
		}
		catch (Exception e)
		{
		    GamerLogger.logStackTrace("GamePlayer", e);
			throw new MetaGamingException();
		}
	}

	@Override
	public final GdlSentence selectMove(long timeout) throws MoveSelectionException
	{
		try
		{
			stateMachine.doPerMoveWork();

			List<GdlSentence> lastMoves = getMatch().getMostRecentMoves();
			if (lastMoves != null)
			{
				List<Move> moves = new ArrayList<Move>();
				for (GdlSentence sentence : lastMoves)
				{
					moves.add(stateMachine.getMoveFromSentence(sentence));
				}

				currentState = stateMachine.getNextState(currentState, moves);
			}

			return stateMachineSelectMove(timeout).getContents();
		}
		catch (Exception e)
		{
		    GamerLogger.logStackTrace("GamePlayer", e);
			throw new MoveSelectionException();
		}
	}

	public abstract void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;

	public abstract Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;

	/* Helper methods for children */
    protected final void cleanupAfterMatch() {
        role = null;
        currentState = null;        
        stateMachine = null;
        setMatch(null);
        setRoleName(null);
    }
    
    protected final void switchStateMachine(StateMachine newStateMachine) {
        try {        
            MachineState newCurrentState = newStateMachine.getInitialState();
            Role newRole = newStateMachine.getRoleFromProp(getRoleName());

            // Attempt to run through the game history in the new machine
            List<List<GdlSentence>> theMoveHistory = getMatch().getHistory();
            for(List<GdlSentence> nextMove : theMoveHistory) {
                List<Move> theJointMove = new ArrayList<Move>();
                for(GdlSentence theSentence : nextMove)
                    theJointMove.add(newStateMachine.getMoveFromSentence(theSentence));                    
                newCurrentState = newStateMachine.getNextStateDestructively(newCurrentState, theJointMove);
            }
            
            // Finally, switch over if everything went well.
            role = newRole;
            currentState = newCurrentState;            
            stateMachine = newStateMachine;
        } catch (Exception e) {
            GamerLogger.log("GamePlayer", "Caught an exception while switching state machine!");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }
}