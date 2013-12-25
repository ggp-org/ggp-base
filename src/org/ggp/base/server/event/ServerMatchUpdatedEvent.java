package org.ggp.base.server.event;

import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;

public final class ServerMatchUpdatedEvent extends Event {
	private final Match match;
	private final String externalPublicationKey;
	private final String externalFilename;

	public ServerMatchUpdatedEvent(Match match, String externalPublicationKey, String externalFilename) {
		this.match = match;
		this.externalFilename = externalFilename;
		this.externalPublicationKey = externalPublicationKey;
	}

	public Match getMatch() {
		return match;
	}

	public String getExternalFilename() {
		return externalFilename;
	}

	public String getExternalPublicationKey() {
		return externalPublicationKey;
	}
}
