package org.ggp.base.apps.server.scheduling;

import java.util.List;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.presence.PlayerPresence;

public final class PendingMatch
{
	public final Game theGame;
	public final List<PlayerPresence> thePlayers;
	public final String matchID;
	public final int previewClock;
	public final int startClock;
	public final int playClock;
	public final boolean shouldScramble;
	public final boolean shouldQueue;
	public final boolean shouldDetail;
	public final boolean shouldSave;
	public final boolean shouldPublish;

	public PendingMatch(String matchIdPrefix, Game theGame, List<PlayerPresence> thePlayers, int previewClock, int startClock, int playClock, boolean shouldScramble, boolean shouldQueue, boolean shouldDetail, boolean shouldSave, boolean shouldPublish) {
		this.matchID = matchIdPrefix + "." + theGame.getKey() + "." + System.currentTimeMillis();
		this.theGame = theGame;
		this.thePlayers = thePlayers;
		this.previewClock = previewClock;
		this.startClock = startClock;
		this.playClock = playClock;
		this.shouldScramble = shouldScramble;
		this.shouldQueue = shouldQueue;
		this.shouldDetail = shouldDetail;
		this.shouldSave = shouldSave;
		this.shouldPublish = shouldPublish;
	}
}