package util.gdl.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlVariable;
import util.gdl.transforms.ConstantFinder.ConstantChecker;

public interface SentenceModel extends SentenceFormSource {

	Set<SentenceForm> getIndependentSentenceForms();

	Set<SentenceForm> getConstantSentenceForms();

	Map<SentenceForm, Set<SentenceForm>> getDependencyGraph();

	Set<GdlRelation> getRelations(SentenceForm form);

	Set<GdlRule> getRules(SentenceForm form);

	Set<SentenceForm> getSentenceForms();

	void restrictDomainsToUsefulValues(ConstantChecker checker) throws InterruptedException;

	Set<String> getSentenceNames();

	Map<GdlVariable, Set<GdlConstant>> getVarDomains(GdlRule rule);

	List<Gdl> getDescription();

	Iterator<GdlSentence> getSentenceIterator(SentenceForm form);

	Iterable<GdlSentence> getSentenceIterable(SentenceForm form);

}
