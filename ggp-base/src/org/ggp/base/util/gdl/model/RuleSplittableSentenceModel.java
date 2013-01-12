package org.ggp.base.util.gdl.model;

import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlRule;


public interface RuleSplittableSentenceModel extends SentenceModel {

	void replaceRules(List<GdlRule> oldRules, List<GdlRule> newRules) throws InterruptedException;

}
