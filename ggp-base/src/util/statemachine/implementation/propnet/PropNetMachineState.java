package util.statemachine.implementation.propnet;

import java.util.Set;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;

public class PropNetMachineState extends MachineState {
	
	private Set<GdlSentence> contents;
	public PropNetMachineState(Set<GdlSentence> contents)
	{
		this.contents = contents;
	}
	@Override
	public Set<GdlSentence> getContents() {
		return contents;
	}

}
