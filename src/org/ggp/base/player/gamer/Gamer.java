package org.ggp.base.player.gamer;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.player.config.ConfigPanel;
import org.ggp.base.apps.player.config.EmptyConfigPanel;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.EmptyDetailPanel;
import org.ggp.base.player.gamer.exception.AbortingException;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
import org.ggp.base.player.gamer.exception.StoppingException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;


/**
 * The Gamer class defines methods for both meta-gaming and move selection in a
 * pre-specified amount of time. The Gamer class is based on the <i>algorithm</i>
 * design pattern.
 */
public abstract class Gamer implements Subject
{
	private Match match;
	private GdlConstant roleName;

	public Gamer()
	{
		observers = new ArrayList<Observer>();

		// When not playing a match, the variables 'match'
		// and 'roleName' should be NULL. This indicates that
		// the player is available for starting a new match.
		match = null;
		roleName = null;
	}

	/* The following values are recommendations to the implementations
	 * for the minimum length of time to leave between the stated timeout
	 * and when you actually return from metaGame and selectMove. They are
	 * stored here so they can be shared amongst all Gamers. */
    public static final long PREFERRED_METAGAME_BUFFER = 3900;
    public static final long PREFERRED_PLAY_BUFFER = 1900;

	// ==== The Gaming Algorithms ====
	public abstract void metaGame(long timeout) throws MetaGamingException;

	public abstract GdlTerm selectMove(long timeout) throws MoveSelectionException;

	/* Note that the match's goal values will not necessarily be known when
	 * stop() is called, as we only know the final set of moves and haven't
	 * interpreted them yet. To get the final goal values, process the final
	 * moves of the game.
	 */
	public abstract void stop() throws StoppingException;  // Cleanly stop playing the match

	public abstract void abort() throws AbortingException;  // Abruptly stop playing the match

	public abstract void preview(Game g, long timeout) throws GamePreviewException;  // Preview a game

	// ==== Gamer Profile and Configuration ====
	public abstract String getName();
	public String getSpecies() { return null; }

	public boolean isComputerPlayer() {
		return true;
	}

	public ConfigPanel getConfigPanel() {
		return new EmptyConfigPanel();
	}

	public DetailPanel getDetailPanel() {
		return new EmptyDetailPanel();
	}

	// ==== Accessors ====
	public final Match getMatch() {
		return match;
	}

	public final void setMatch(Match match) {
		this.match = match;
	}

	public final GdlConstant getRoleName() {
		return roleName;
	}

	public final void setRoleName(GdlConstant roleName) {
		this.roleName = roleName;
	}

	// ==== Observer Stuff ====
	private final List<Observer> observers;
	@Override
	public final void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	@Override
	public final void notifyObservers(Event event)
	{
		for (Observer observer : observers) {
			observer.observe(event);
		}
	}
}