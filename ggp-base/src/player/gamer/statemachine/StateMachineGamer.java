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

/**
 * The base class for Gamers that utilize state machines.  Any reasonable GGP player should subclass this class.
 * See @SimpleSearchLightGamer, @HumanGamer, and @LegalGamer for examples.
 * @author evancox
 *
 */
public abstract class StateMachineGamer extends Gamer
{
	private MachineState currentState;
	private Role role;
	private StateMachine stateMachine;
	
	/**
	 * Returns the current state
	 * @return the current state
	 */

	public final MachineState getCurrentState()
	{
		return currentState;
	}
	
	/**
	 * Returns the current role
	 * @return the current role
	 */

	public final Role getRole()
	{
		return role;
	}
	
	/**
	 * Returns the state machine.  This is used for calculating the next state and other operations such as computing
	 * the legal moves for all players
	 * 
	 * @return a state machine
	 */

	public final StateMachine getStateMachine()
	{
		return stateMachine;
	}
	
	/**
	 * A wrapper function for stateMachineMetaGame.  Initializes the state machine and
	 * role for the player using the match description, before calling stateMachineMetaGame
	 */
	
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
	
	/**
	 * A wrapper function to stateMachineSelectMove. Advances the state machine to the next state so that
	 * stateMachineSelectMove can operate properly
	 */

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
	
	/**
	 * Defines the metagaming action taken by a player during the START_CLOCK
	 * @param timeout the START_CLOCK for the current game
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException
	 * @throws GoalDefinitionException
	 */
	
	public abstract void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;
	
	/**
	 * Defines the algorithm that the player uses to select their move.
	 * @param timeout the START_CLOCK for the current game
	 * @return Move - the move selected by the player
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException
	 * @throws GoalDefinitionException
	 */
	public abstract Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;

	/**
	 * Cleans up the role, currentState and stateMachine.
	 */
	/* Helper methods for children */
    protected final void cleanupAfterMatch() {
        role = null;
        currentState = null;        
        stateMachine = null;
        setMatch(null);
        setRoleName(null);
    }
    
    /**
     * Switches stateMachine to newStateMachine
     * @param newStateMachine the new state machine
     */
    
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