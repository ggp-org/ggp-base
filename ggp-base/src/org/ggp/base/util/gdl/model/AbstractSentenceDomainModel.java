package org.ggp.base.util.gdl.model;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;

import com.google.common.collect.Multimap;

/**
 * Allows SentenceDomainModels to delegate their SentenceFormModel aspects
 * to an existing SentenceFormModel.
 */
public abstract class AbstractSentenceDomainModel implements SentenceDomainModel {
	private final SentenceFormModel formModel;

	protected AbstractSentenceDomainModel(SentenceFormModel formModel) {
		this.formModel = formModel;
	}

	/*package-private*/ SentenceFormModel getFormModel() {
		return formModel;
	}

	@Override
	public Set<SentenceForm> getIndependentSentenceForms() {
		return formModel.getIndependentSentenceForms();
	}

	@Override
	public Set<SentenceForm> getConstantSentenceForms() {
		return formModel.getConstantSentenceForms();
	}

	@Override
	public Multimap<SentenceForm, SentenceForm> getDependencyGraph() {
		return formModel.getDependencyGraph();
	}

	@Override
	public Set<GdlSentence> getSentencesListedAsTrue(SentenceForm form) {
		return formModel.getSentencesListedAsTrue(form);
	}

	@Override
	public Set<GdlRule> getRules(SentenceForm form) {
		return formModel.getRules(form);
	}

	@Override
	public Set<SentenceForm> getSentenceForms() {
		return formModel.getSentenceForms();
	}

	@Override
	public List<Gdl> getDescription() {
		return formModel.getDescription();
	}

	@Override
	public SentenceForm getSentenceForm(GdlSentence sentence) {
		return formModel.getSentenceForm(sentence);
	}
}
