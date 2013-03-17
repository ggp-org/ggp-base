package org.ggp.base.server.event;

import java.io.Serializable;

import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;

@SuppressWarnings("serial")
public final class ServerMatchUpdatedEvent extends Event implements Serializable {
	private final Match match;

	public ServerMatchUpdatedEvent(Match match) {
		this.match = match;
	}

	public Match getMatch() {
		return match;
	}
}
