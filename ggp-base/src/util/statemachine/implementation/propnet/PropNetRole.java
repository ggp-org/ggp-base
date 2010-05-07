package util.statemachine.implementation.propnet;

import util.gdl.grammar.GdlProposition;
import util.statemachine.Role;

public class PropNetRole implements Role {	
	private GdlProposition name;

	public PropNetRole(GdlProposition name)
	{
		this.name = name;
	}
	
	@Override
	public GdlProposition getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if ((o != null) && (o instanceof PropNetRole))
		{
			PropNetRole role = (PropNetRole) o;
			return role.name.equals(name);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public String toString()
	{
		return name.toString();
	}
}