package util.gdl;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlOr;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;

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
	//TODO: Do we also want for Gdl, GdlTerm, GdlLiteral?
	//For sentence: Break down into GdlRelation, GdlProposition?
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
