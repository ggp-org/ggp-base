package org.ggp.base.util.gdl.scrambler;

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
 * A renderer for Gdl objects. On its own, this class renders a Gdl object
 * in the way you'd expect. It can be subclassed to override particular parts
 * of the rendering scheme; for example, to render GdlConstants scrambled via
 * a mapping.
 * 
 * TODO(schreib): Would it ever make sense for this to replace the regular
 * toString methods in the Gdl objects?
 * 
 * TODO(schreib): What is the relationship between this and the GdlVisitor
 * framework that Alex put together? Can they be combined?
 * 
 * @author Sam Schreiber
 */
public class GdlRenderer {
	public String renderGdl(Gdl gdl) {
		if (gdl instanceof GdlTerm) {
			return renderTerm((GdlTerm) gdl);
		} else if (gdl instanceof GdlLiteral) {
			return renderLiteral((GdlLiteral) gdl);
		} else if (gdl instanceof GdlRule) {
			return renderRule((GdlRule) gdl);
		} else {
			throw new RuntimeException("Unexpected Gdl type " + gdl.getClass());
		}
	}
	protected String renderTerm(GdlTerm term) {
		if (term instanceof GdlConstant) {
			return renderConstant((GdlConstant) term);
		} else if (term instanceof GdlVariable) {
			return renderVariable((GdlVariable) term);
		} else if (term instanceof GdlFunction) {
			return renderFunction((GdlFunction) term);
		} else {
			throw new RuntimeException("Unexpected GdlTerm type " + term.getClass());
		}
	}
	protected String renderSentence(GdlSentence sentence) {
		if (sentence instanceof GdlProposition) {
			return renderProposition((GdlProposition) sentence);
		} else if (sentence instanceof GdlRelation) {
			return renderRelation((GdlRelation) sentence);
		} else {
			throw new RuntimeException("Unexpected GdlSentence type " + sentence.getClass());
		}
	}
	protected String renderLiteral(GdlLiteral literal) {
		if (literal instanceof GdlSentence) {
			return renderSentence((GdlSentence) literal);
		} else if (literal instanceof GdlNot) {
			return renderNot((GdlNot) literal);
		} else if (literal instanceof GdlOr) {
			return renderOr((GdlOr) literal);
		} else if (literal instanceof GdlDistinct) {
			return renderDistinct((GdlDistinct) literal);
		} else {
			throw new RuntimeException("Unexpected GdlLiteral type " + literal.getClass());
		}
	}
	protected String renderConstant(GdlConstant constant) {
		return constant.toString();
	}
	protected String renderVariable(GdlVariable variable) {
		return variable.toString();
	}
	protected String renderFunction(GdlFunction function) {
		StringBuilder sb = new StringBuilder();

		sb.append("( " + renderConstant(function.getName()) + " ");
		for (GdlTerm term : function.getBody())
		{
			sb.append(renderTerm(term) + " ");
		}
		sb.append(")");

		return sb.toString();
	}
	protected String renderRelation(GdlRelation relation) {
		StringBuilder sb = new StringBuilder();

		sb.append("( " + renderConstant(relation.getName()) + " ");
		for (GdlTerm term : relation.getBody())
		{
			sb.append(renderTerm(term) + " ");
		}
		sb.append(")");

		return sb.toString();
	}
	protected String renderProposition(GdlProposition proposition) {
		return renderConstant(proposition.getName());
	}
	protected String renderNot(GdlNot not) {
		return "( not " + renderLiteral(not.getBody()) + " )";
	}
	protected String renderDistinct(GdlDistinct distinct) {
		return "( distinct " + renderTerm(distinct.getArg1()) + " " + renderTerm(distinct.getArg2()) + " )";
	}
	protected String renderOr(GdlOr or) {
		StringBuilder sb = new StringBuilder();

		sb.append("( or ");
		for (int i = 0; i < or.arity(); i++)
		{
			sb.append(renderLiteral(or.get(i)) + " ");
		}
		sb.append(")");

		return sb.toString();
	}
	protected String renderRule(GdlRule rule) {
		StringBuilder sb = new StringBuilder();

		sb.append("( <= " + renderSentence(rule.getHead()) + " ");
		for (GdlLiteral literal : rule.getBody())
		{
			sb.append(renderLiteral(literal) + " ");
		}
		sb.append(")");

		return sb.toString();
	}
}
