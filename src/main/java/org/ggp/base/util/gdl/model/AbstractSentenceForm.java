package org.ggp.base.util.gdl.model;

import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlSentence;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 * Defines the hashCode, equals, and toString methods for SentenceForms so
 * different SentenceForms can be compatible in terms of how they treat these
 * methods. SentenceForm implementations should extend this class and should
 * not reimplement hashCode, equals, or toString.
 */
public abstract class AbstractSentenceForm implements SentenceForm {
    private final Supplier<GdlSentence> underscoreSentence =
            Suppliers.memoize(new Supplier<GdlSentence>() {
                @Override
                public GdlSentence get() {
                    List<GdlConstant> underscores = getNUnderscores(getTupleSize());
                    return getSentenceFromTuple(underscores);
                }
            });

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SentenceForm)) {
            return false;
        }
        SentenceForm o = (SentenceForm) obj;
        if (this.getName() != o.getName()) {
            return false;
        }
        if (this.getTupleSize() != o.getTupleSize()) {
            return false;
        }
        return o.matches(underscoreSentence.get());
    }

    private static List<GdlConstant> getNUnderscores(int numTerms) {
        GdlConstant underscore = GdlPool.UNDERSCORE;
        List<GdlConstant> terms = Lists.newArrayListWithCapacity(numTerms);
        for (int i = 0; i < numTerms; i++) {
            terms.add(underscore);
        }
        return terms;
    }

    private volatile int hashCode = 0;
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = toString().hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return underscoreSentence.get().toString();
    }
}
