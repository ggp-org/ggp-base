package org.ggp.base.util.gdl.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.transforms.ConstantFinder.ConstantCheckerImpl;

import com.google.common.collect.Multimap;


public interface SentenceModel extends SentenceFormSource, SentenceDomainModel {

	Set<SentenceForm> getIndependentSentenceForms();

	Set<SentenceForm> getConstantSentenceForms();

	Multimap<SentenceForm, SentenceForm> getDependencyGraph();

	Set<GdlSentence> getSentencesListedAsTrue(SentenceForm form);

	Set<GdlRule> getRules(SentenceForm form);

	Set<SentenceForm> getSentenceForms();

	void restrictDomainsToUsefulValues(ConstantCheckerImpl checker) throws InterruptedException;

	Set<String> getSentenceNames();

	Map<GdlVariable, Set<GdlConstant>> getVarDomains(GdlRule rule);

	List<Gdl> getDescription();

	Iterator<GdlSentence> getSentenceIterator(SentenceForm form);

	Iterable<GdlSentence> getSentenceIterable(SentenceForm form);

}
