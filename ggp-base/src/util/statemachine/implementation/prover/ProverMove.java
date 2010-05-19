package util.statemachine.implementation.prover;

import util.gdl.grammar.GdlSentence;
import util.statemachine.Move;

@SuppressWarnings("serial")
public final class ProverMove extends Move
{
    public ProverMove(GdlSentence contents) {
        super(contents);
    }
}