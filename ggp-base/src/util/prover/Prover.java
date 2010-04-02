package util.prover;

import java.util.Set;
import util.gdl.grammar.GdlSentence;

public abstract class Prover
{
	public abstract Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context);
	public abstract GdlSentence askOne(GdlSentence query, Set<GdlSentence> context);
	public abstract boolean prove(GdlSentence query, Set<GdlSentence> context);
}
