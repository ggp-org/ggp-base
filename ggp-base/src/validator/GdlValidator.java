package validator;

import java.util.ArrayList;
import java.util.List;

import util.gdl.grammar.Gdl;
import util.observer.Event;
import util.observer.Observer;
import util.observer.Subject;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import validator.event.ValidatorFailureEvent;
import validator.event.ValidatorSuccessEvent;
import validator.exception.MaxDepthException;
import validator.exception.MonotonicityException;

public final class GdlValidator extends Thread implements Subject
{

	private final List<Gdl> description;
	private final int maxDepth;
	private final int numSimulations;
	private final List<Observer> observers;

	public GdlValidator(List<Gdl> description, int maxDepth, int numSimulations)
	{
		this.description = description;
		this.maxDepth = maxDepth;
		this.numSimulations = numSimulations;
		observers = new ArrayList<Observer>();
	}

	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	public void notifyObservers(Event event)
	{
		for (Observer observer : observers)
		{
			observer.observe(event);
		}
	}

	@Override
	public void run()
	{
		for (int i = 0; i < numSimulations; i++)
		{
			simulate();
		}
	}

	private void simulate()
	{
		try
		{
			StateMachine stateMachine = new ProverStateMachine();
			stateMachine.initialize(description);
			List<Role> roles = stateMachine.getRoles();

			List<Integer> goals = new ArrayList<Integer>();
			for (int i = 0; i < roles.size(); i++)
			{
				goals.add(-1);
			}

			MachineState state = stateMachine.getInitialState();
			for (int depth = 0; !stateMachine.isTerminal(state); depth++)
			{
				if (depth == maxDepth)
				{
					throw new MaxDepthException(maxDepth);
				}

				for (int i = 0; i < roles.size(); i++)
				{
					int goal = stateMachine.getGoal(state, roles.get(i));
					if (goal < goals.get(i))
					{
						throw new MonotonicityException(roles.get(i));
					}
					goals.set(i, goal);
				}

				state = stateMachine.getRandomNextState(state);
			}

			notifyObservers(new ValidatorSuccessEvent());
		}
		catch (Exception e)
		{
			notifyObservers(new ValidatorFailureEvent(e));
		}
	}

}
