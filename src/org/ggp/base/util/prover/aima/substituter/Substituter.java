package org.ggp.base.util.prover.aima.substituter;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.prover.aima.substitution.Substitution;


public final class Substituter
{

	public static GdlLiteral substitute(GdlLiteral literal, Substitution theta)
	{
		return substituteLiteral(literal, theta);
	}

	public static GdlSentence substitute(GdlSentence sentence, Substitution theta)
	{
		return substituteSentence(sentence, theta);
	}

	public static GdlRule substitute(GdlRule rule, Substitution theta)
	{
		return substituteRule(rule, theta);
	}

	private static GdlConstant substituteConstant(GdlConstant constant, Substitution theta)
	{
		return constant;
	}

	private static GdlDistinct substituteDistinct(GdlDistinct distinct, Substitution theta)
	{
		if (distinct.isGround())
		{
			return distinct;
		}
		else
		{
			GdlTerm arg1 = substituteTerm(distinct.getArg1(), theta);
			GdlTerm arg2 = substituteTerm(distinct.getArg2(), theta);

			return GdlPool.getDistinct(arg1, arg2);
		}
	}

	private static GdlFunction substituteFunction(GdlFunction function, Substitution theta)
	{
		if (function.isGround())
		{
			return function;
		}
		else
		{
			GdlConstant name = substituteConstant(function.getName(), theta);

			List<GdlTerm> body = new ArrayList<GdlTerm>();
			for (int i = 0; i < function.arity(); i++)
			{
				body.add(substituteTerm(function.get(i), theta));
			}

			return GdlPool.getFunction(name, body);
		}
	}

	private static GdlLiteral substituteLiteral(GdlLiteral literal, Substitution theta)
	{
		if (literal instanceof GdlDistinct)
		{
			return substituteDistinct((GdlDistinct) literal, theta);
		}
		else if (literal instanceof GdlNot)
		{
			return substituteNot((GdlNot) literal, theta);
		}
		else if (literal instanceof GdlOr)
		{
			return substituteOr((GdlOr) literal, theta);
		}
		else
		{
			return substituteSentence((GdlSentence) literal, theta);
		}
	}

	private static GdlNot substituteNot(GdlNot not, Substitution theta)
	{
		if (not.isGround())
		{
			return not;
		}
		else
		{
			GdlLiteral body = substituteLiteral(not.getBody(), theta);
			return GdlPool.getNot(body);
		}
	}

	private static GdlOr substituteOr(GdlOr or, Substitution theta)
	{
		if (or.isGround())
		{
			return or;
		}
		else
		{
			List<GdlLiteral> disjuncts = new ArrayList<GdlLiteral>();
			for (int i = 0; i < or.arity(); i++)
			{
				disjuncts.add(substituteLiteral(or.get(i), theta));
			}

			return GdlPool.getOr(disjuncts);
		}
	}

	private static GdlProposition substituteProposition(GdlProposition proposition, Substitution theta)
	{
		return proposition;
	}

	private static GdlRelation substituteRelation(GdlRelation relation, Substitution theta)
	{
		if (relation.isGround())
		{
			return relation;
		}
		else
		{
			GdlConstant name = substituteConstant(relation.getName(), theta);

			List<GdlTerm> body = new ArrayList<GdlTerm>();
			for (int i = 0; i < relation.arity(); i++)
			{
				body.add(substituteTerm(relation.get(i), theta));
			}

			return GdlPool.getRelation(name, body);
		}
	}

	private static GdlSentence substituteSentence(GdlSentence sentence, Substitution theta)
	{
		if (sentence instanceof GdlProposition)
		{
			return substituteProposition((GdlProposition) sentence, theta);
		}
		else
		{
			return substituteRelation((GdlRelation) sentence, theta);
		}
	}

	private static GdlTerm substituteTerm(GdlTerm term, Substitution theta)
	{
		if (term instanceof GdlConstant)
		{
			return substituteConstant((GdlConstant) term, theta);
		}
		else if (term instanceof GdlVariable)
		{
			return substituteVariable((GdlVariable) term, theta);
		}
		else
		{
			return substituteFunction((GdlFunction) term, theta);
		}
	}

	private static GdlTerm substituteVariable(GdlVariable variable, Substitution theta)
	{
		if (!theta.contains(variable))
		{
			return variable;
		}
		else
		{
			GdlTerm result = theta.get(variable);
			GdlTerm betterResult = null;

			while (!(betterResult = substituteTerm(result, theta)).equals(result))
			{
				result = betterResult;
			}

			theta.put(variable, result);
			return result;
		}
	}

	private static GdlRule substituteRule(GdlRule rule, Substitution theta)
	{
		GdlSentence head = substitute(rule.getHead(), theta);

		List<GdlLiteral> body = new ArrayList<GdlLiteral>();
		for ( GdlLiteral literal : rule.getBody() )
		{
			body.add(substituteLiteral(literal, theta));
		}

		return GdlPool.getRule(head, body);
	}
}
