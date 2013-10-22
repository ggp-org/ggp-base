package org.ggp.base.apps.validator.event;

import org.ggp.base.util.observer.Event;

public final class ValidatorSuccessEvent extends Event
{
	private final String name;

	public ValidatorSuccessEvent(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
}
