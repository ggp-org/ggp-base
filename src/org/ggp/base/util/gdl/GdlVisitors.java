package org.ggp.base.util.gdl;

import java.util.Collection;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;


public class GdlVisitors {
	/**
	 * Visits the given Gdl object and any contained Gdl objects within
	 * with the given GdlVisitor. For example, when called on a GdlRule,
	 * the visitor's visitConstant function is called once for every
	 * constant anywhere in the head or body of the rule.
	 * 
	 * @author Alex Landau
	 */
	public static void visitAll(Gdl gdl, GdlVisitor visitor) {
		visitor.visitGdl(gdl);
		if (gdl instanceof GdlTerm) {
			visitTerm((GdlTerm) gdl, visitor);
		} else if (gdl instanceof GdlLiteral) {
			visitLiteral((GdlLiteral) gdl, visitor);
		} else if (gdl instanceof GdlRule) {
			visitRule((GdlRule) gdl, visitor);
		} else {
			throw new RuntimeException("Unexpected Gdl type " + gdl.getClass());
		}
	}
	public static void visitAll(Collection<? extends Gdl> collection, GdlVisitor visitor) {
		for (Gdl gdl : collection) {
			visitAll(gdl, visitor);
		}
	}
	private static void visitRule(GdlRule rule, GdlVisitor visitor) {
		visitor.visitRule(rule);
		visitAll(rule.getHead(), visitor);
		visitAll(rule.getBody(), visitor);
	}
	private static void visitLiteral(GdlLiteral literal, GdlVisitor visitor) {
		visitor.visitLiteral(literal);
		if (literal instanceof GdlSentence) {
			visitSentence((GdlSentence) literal, visitor);
		} else if (literal instanceof GdlNot) {
			visitNot((GdlNot) literal, visitor);
		} else if (literal instanceof GdlOr) {
			visitOr((GdlOr) literal, visitor);
		} else if (literal instanceof GdlDistinct) {
			visitDistinct((GdlDistinct) literal, visitor);
		} else {
			throw new RuntimeException("Unexpected GdlLiteral type " + literal.getClass());
		}
	}
	private static void visitDistinct(GdlDistinct distinct, GdlVisitor visitor) {
		visitor.visitDistinct(distinct);
		visitAll(distinct.getArg1(), visitor);
		visitAll(distinct.getArg2(), visitor);
	}
	private static void visitOr(GdlOr or, GdlVisitor visitor) {
		visitor.visitOr(or);
		for (int i = 0; i < or.arity(); i++) {
			visitAll(or.get(i), visitor);
		}
	}
	private static void visitNot(GdlNot not, GdlVisitor visitor) {
		visitor.visitNot(not);
		visitAll(not.getBody(), visitor);
	}
	private static void visitSentence(GdlSentence sentence, GdlVisitor visitor) {
		visitor.visitSentence(sentence);
		if (sentence instanceof GdlProposition) {
			visitProposition((GdlProposition) sentence, visitor);
		} else if (sentence instanceof GdlRelation) {
			visitRelation((GdlRelation) sentence, visitor);
		} else {
			throw new RuntimeException("Unexpected GdlSentence type " + sentence.getClass());
		}
	}
	private static void visitRelation(GdlRelation relation, GdlVisitor visitor) {
		visitor.visitRelation(relation);
		visitAll(relation.getName(), visitor);
		visitAll(relation.getBody(), visitor);
	}
	private static void visitProposition(GdlProposition proposition,
			GdlVisitor visitor) {
		visitor.visitProposition(proposition);
		visitAll(proposition.getName(), visitor);
	}
	private static void visitTerm(GdlTerm term, GdlVisitor visitor) {
		visitor.visitTerm(term);
		if (term instanceof GdlConstant) {
			visitor.visitConstant((GdlConstant) term);
		} else if (term instanceof GdlVariable) {
			visitor.visitVariable((GdlVariable) term);
		} else if (term instanceof GdlFunction) {
			visitFunction((GdlFunction) term, visitor);
		} else {
			throw new RuntimeException("Unexpected GdlTerm type " + term.getClass());
		}
	}
	private static void visitFunction(GdlFunction function, GdlVisitor visitor) {
		visitor.visitFunction(function);
		visitAll(function.getName(), visitor);
		visitAll(function.getBody(), visitor);
	}
}
