package util.statemachine;

import util.gdl.grammar.GdlProposition;

/**
 * Provides the interface for Roles.  Really simple,
 * it just lets you get a GdlProposition representing
 * the name of the role.
 */
public interface Role {

	public abstract GdlProposition getName();

}