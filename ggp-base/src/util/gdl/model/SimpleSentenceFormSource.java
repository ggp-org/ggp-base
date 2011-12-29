package util.gdl.model;

import util.gdl.grammar.GdlSentence;

public class SimpleSentenceFormSource implements SentenceFormSource {

	@Override
	public SentenceForm getSentenceForm(GdlSentence sentence) {
		return new SimpleSentenceForm(sentence);
	}

}
