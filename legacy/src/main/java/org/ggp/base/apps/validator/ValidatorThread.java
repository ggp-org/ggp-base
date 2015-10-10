package org.ggp.base.apps.validator;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.validator.event.ValidatorFailureEvent;
import org.ggp.base.apps.validator.event.ValidatorSuccessEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;
import org.ggp.base.validator.GameValidator;
import org.ggp.base.validator.ValidatorException;
import org.ggp.base.validator.ValidatorWarning;

public final class ValidatorThread extends Thread implements Subject
{
	private final Game theGame;
	private final GameValidator theValidator;
	private final List<Observer> observers;

	public ValidatorThread(Game theGame, GameValidator theValidator)
	{
		this.theGame = theGame;
		this.theValidator = theValidator;
		this.observers = new ArrayList<Observer>();
	}

	@Override
	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	@Override
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
		try {
			List<ValidatorWarning> warnings = theValidator.checkValidity(theGame);
			notifyObservers(new ValidatorSuccessEvent(theValidator.getClass().getSimpleName(), warnings));
		} catch (ValidatorException ve) {
			notifyObservers(new ValidatorFailureEvent(theValidator.getClass().getSimpleName(), ve));
		}
	}
}
