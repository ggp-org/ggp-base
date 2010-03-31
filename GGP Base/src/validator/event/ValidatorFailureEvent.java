package validator.event;

import util.observer.Event;

public class ValidatorFailureEvent extends Event
{

	private final Exception exception;

	public ValidatorFailureEvent(Exception exception)
	{
		this.exception = exception;
	}

	public Exception getException()
	{
		return exception;
	}

}
