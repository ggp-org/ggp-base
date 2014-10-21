package org.ggp.base.util.propnet.factory;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.factory.converter.PropNetConverter;
import org.ggp.base.util.propnet.factory.flattener.PropNetFlattener;
import org.ggp.base.util.statemachine.Role;


/**
 * The LegacyPropNetFactory class defines the creation of PropNets from game
 * descriptions. It has been superseded by the @OptimizingPropNetFactory
 * which has a more sophisticated approach to creating PropNets.
 */
public final class LegacyPropNetFactory
{
	/**
	 * Creates a PropNet from a game description using the following process:
	 * <ol>
	 * <li>Flattens the game description to remove variables.</li>
	 * <li>Converts the flattened description into an equivalent PropNet.</li>
	 * </ol>
	 *
	 * @param description
	 *            A game description.
	 * @return An equivalent PropNet.
	 */
	public static PropNet create(List<Gdl> description)
	{
        try {
            List<GdlRule> flatDescription = new PropNetFlattener(description).flatten();
            GamerLogger.log("StateMachine", "Converting...");
            return new PropNetConverter().convert(Role.computeRoles(description), flatDescription);
        } catch(Exception e) {
            GamerLogger.logStackTrace("StateMachine", e);
            return null;
        }
	}
}