package util.statemachine.implementation.prover;

import java.io.Serializable;
import java.util.Set;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;

@SuppressWarnings("serial")
public final class ProverMachineState extends MachineState implements Serializable
{
	private final Set<GdlSentence> contents;

	public ProverMachineState(Set<GdlSentence> contents)
	{
		this.contents = contents;
	}

	/* (non-Javadoc)
	 * @see util.statemachine.prover.MachineState#getContents()
	 */
	public Set<GdlSentence> getContents()
	{
		return contents;
	}
}
