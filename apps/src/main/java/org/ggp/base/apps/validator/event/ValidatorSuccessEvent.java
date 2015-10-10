package org.ggp.base.apps.validator.event;

import java.util.List;

import org.ggp.base.util.observer.Event;
import org.ggp.base.validator.ValidatorWarning;

public final class ValidatorSuccessEvent extends Event
{
	private final String name;
	private final List<ValidatorWarning> warnings;

	public ValidatorSuccessEvent(String name, List<ValidatorWarning> warnings)
	{
		this.name = name;
		this.warnings = warnings;
	}

	public String getName()
	{
		return name;
	}

	public List<ValidatorWarning> getWarnings() {
		return warnings;
	}
}
