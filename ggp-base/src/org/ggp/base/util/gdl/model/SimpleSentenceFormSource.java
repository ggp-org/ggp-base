package org.ggp.base.util.gdl.model;

import org.ggp.base.util.gdl.grammar.GdlSentence;

public class SimpleSentenceFormSource implements SentenceFormSource {

	@Override
	public SentenceForm getSentenceForm(GdlSentence sentence) {
		return new SimpleSentenceForm(sentence);
	}

}
