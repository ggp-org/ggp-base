package org.ggp.base.util.gdl.model;

import java.util.List;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.junit.Assert;
import org.junit.Test;

public class SimpleSentenceFormTest extends Assert {
    @Test
    public void testFunctionNesting() throws Exception {
        GdlSentence sentence = (GdlSentence) GdlFactory.create("(does player (combine foo (bar b b)))");
        SimpleSentenceForm form = SimpleSentenceForm.create(sentence);
        assertEquals(GdlPool.DOES, form.getName());
        assertEquals(4, form.getTupleSize());
        assertTrue(form.matches(sentence));

        List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
        assertEquals(sentence, form.getSentenceFromTuple(tuple));
    }
}
