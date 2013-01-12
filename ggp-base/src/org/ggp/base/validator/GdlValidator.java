package org.ggp.base.validator;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.validator.event.ValidatorFailureEvent;
import org.ggp.base.validator.event.ValidatorSuccessEvent;
import org.ggp.base.validator.exception.MaxDepthException;
import org.ggp.base.validator.exception.MonotonicityException;


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
