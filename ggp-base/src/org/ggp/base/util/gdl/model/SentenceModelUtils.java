package org.ggp.base.util.gdl.model;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;


public class SentenceModelUtils {
	public static boolean inSentenceFormGroup(GdlSentence sentence,
			Set<SentenceForm> forms) {
		for(SentenceForm form : forms)
			if(form.matches(sentence))
				return true;
		return false;
	}

}
