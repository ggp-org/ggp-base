package org.ggp.base.util.gdl.model;

import java.util.List;
import java.util.Map;

import org.ggp.base.util.gdl.grammar.Gdl;

public class SentenceDomainModelFactory {
	public static ImmutableSentenceDomainModel createWithCartesianDomains(List<Gdl> description) throws InterruptedException {
		ImmutableSentenceFormModel formModel = SentenceFormModelFactory.create(description);

		SentenceFormsFinder sentenceFormsFinder = new SentenceFormsFinder(formModel.getDescription());
		Map<SentenceForm, SentenceFormDomain> domains = sentenceFormsFinder.findCartesianDomains();

		return ImmutableSentenceDomainModel.create(formModel, domains);
	}
}
