package org.ggp.base.util.gdl.grammar;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Gdl implements Serializable
{

	public abstract boolean isGround();

	@Override
	public abstract String toString();

}
