package util.gdl.model;

import java.util.Set;

import util.gdl.grammar.GdlSentence;

public class SentenceModelUtils {
	public static boolean inSentenceFormGroup(GdlSentence sentence,
			Set<SentenceForm> forms) {
		for(SentenceForm form : forms)
			if(form.matches(sentence))
				return true;
		return false;
	}

}
