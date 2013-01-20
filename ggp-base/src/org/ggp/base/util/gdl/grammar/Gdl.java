package org.ggp.base.util.gdl.grammar;

import java.io.ObjectStreamException;
import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Gdl implements Serializable
{

	public abstract boolean isGround();

	@Override
	public abstract String toString();

	/**
	 * This method is used by deserialization to ensure that Gdl objects
	 * loaded from an ObjectInputStream or a remote method invocation
	 * are the versions that exist in the GdlPool.
	 */
	protected Object readResolve() throws ObjectStreamException {
		return GdlPool.immerse(this);
	}

}
