package org.ggp.base.apps.validator.event;

import org.ggp.base.util.observer.Event;

public class ValidatorFailureEvent extends Event
{
	private final String name;
	private final Exception exception;

	public ValidatorFailureEvent(String name, Exception exception)
	{
		this.name = name;
		this.exception = exception;
	}

	public String getName()
	{
		return name;
	}

	public Exception getException()
	{
		return exception;
	}

}
