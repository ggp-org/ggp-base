package org.ggp.base.util.gdl;

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

/**
 * A visitor for Gdl objects. The GdlVisitors class has methods for going
 * through a Gdl object or collection thereof and applying the visitor methods
 * to all relevant Gdl objects.
 * 
 * This visitor uses the adapter design pattern, providing empty implementations
 * of each method so subclasses need only implement the relevant methods.
 * 
 * @author Alex Landau
 */
public abstract class GdlVisitor {
	public void visitGdl(Gdl gdl) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitTerm(GdlTerm term) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitConstant(GdlConstant constant) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitVariable(GdlVariable variable) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitFunction(GdlFunction function) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitLiteral(GdlLiteral literal) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitSentence(GdlSentence sentence) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitRelation(GdlRelation relation) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitProposition(GdlProposition proposition) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitNot(GdlNot not) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitDistinct(GdlDistinct distinct) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitOr(GdlOr or) {
		// Do nothing; override in a subclass to do something.
	}
	public void visitRule(GdlRule rule) {
		// Do nothing; override in a subclass to do something.
	}
}
