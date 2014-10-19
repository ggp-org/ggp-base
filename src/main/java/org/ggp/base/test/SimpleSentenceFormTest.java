package org.ggp.base.test;

import java.util.List;

import junit.framework.Assert;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;
import org.junit.Test;

public class SimpleSentenceFormTest {
	@Test
	public void testFunctionNesting() throws Exception {
		GdlSentence sentence = (GdlSentence) GdlFactory.create("(does player (combine foo (bar b b)))");
		SimpleSentenceForm form = SimpleSentenceForm.create(sentence);
		Assert.assertEquals(GdlPool.DOES, form.getName());
		Assert.assertEquals(4, form.getTupleSize());
		Assert.assertTrue(form.matches(sentence));

		List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
		Assert.assertEquals(sentence,
				form.getSentenceFromTuple(tuple));
	}
}
