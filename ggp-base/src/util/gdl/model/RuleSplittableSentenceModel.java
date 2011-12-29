package util.gdl.model;

import java.util.List;

import util.gdl.grammar.GdlRule;

public interface RuleSplittableSentenceModel extends SentenceModel {

	void replaceRules(List<GdlRule> oldRules, List<GdlRule> newRules) throws InterruptedException;

}
