package org.ggp.base.util.prover.aima.renamer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


public class VariableRenamer
{

	private int nextName;

	public VariableRenamer()
	{
		nextName = 0;
	}

	public GdlRule rename(GdlRule rule)
	{
		return renameRule(rule, new HashMap<GdlVariable, GdlVariable>());
	}

	public GdlSentence rename(GdlSentence sentence)
	{
		return renameSentence(sentence, new HashMap<GdlVariable, GdlVariable>());
	}

	private GdlConstant renameConstant(GdlConstant constant, Map<GdlVariable, GdlVariable> renamings)
	{
		return constant;
	}

	private GdlDistinct renameDistinct(GdlDistinct distinct, Map<GdlVariable, GdlVariable> renamings)
	{
		if (distinct.isGround())
		{
			return distinct;
		}
		else
		{
			GdlTerm arg1 = renameTerm(distinct.getArg1(), renamings);
			GdlTerm arg2 = renameTerm(distinct.getArg2(), renamings);

			return GdlPool.getDistinct(arg1, arg2);
		}
	}

	private GdlFunction renameFunction(GdlFunction function, Map<GdlVariable, GdlVariable> renamings)
	{
		if (function.isGround())
		{
			return function;
		}
		else
		{
			GdlConstant name = renameConstant(function.getName(), renamings);

			List<GdlTerm> body = new ArrayList<GdlTerm>();
			for (int i = 0; i < function.arity(); i++)
			{
				body.add(renameTerm(function.get(i), renamings));
			}

			return GdlPool.getFunction(name, body);
		}
	}

	private GdlLiteral renameLiteral(GdlLiteral literal, Map<GdlVariable, GdlVariable> renamings)
	{
		if (literal instanceof GdlDistinct)
		{
			return renameDistinct((GdlDistinct) literal, renamings);
		}
		else if (literal instanceof GdlNot)
		{
			return renameNot((GdlNot) literal, renamings);
		}
		else if (literal instanceof GdlOr)
		{
			return renameOr((GdlOr) literal, renamings);
		}
		else
		{
			return renameSentence((GdlSentence) literal, renamings);
		}
	}

	private GdlNot renameNot(GdlNot not, Map<GdlVariable, GdlVariable> renamings)
	{
		if (not.isGround())
		{
			return not;
		}
		else
		{
			GdlLiteral body = renameLiteral(not.getBody(), renamings);
			return GdlPool.getNot(body);
		}
	}

	private GdlOr renameOr(GdlOr or, Map<GdlVariable, GdlVariable> renamings)
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
				disjuncts.add(renameLiteral(or.get(i), renamings));
			}

			return GdlPool.getOr(disjuncts);
		}
	}

	private GdlProposition renameProposition(GdlProposition proposition, Map<GdlVariable, GdlVariable> renamings)
	{
		return proposition;
	}

	private GdlRelation renameRelation(GdlRelation relation, Map<GdlVariable, GdlVariable> renamings)
	{
		if (relation.isGround())
		{
			return relation;
		}
		else
		{
			GdlConstant name = renameConstant(relation.getName(), renamings);

			List<GdlTerm> body = new ArrayList<GdlTerm>();
			for (int i = 0; i < relation.arity(); i++)
			{
				body.add(renameTerm(relation.get(i), renamings));
			}

			return GdlPool.getRelation(name, body);
		}
	}

	private GdlRule renameRule(GdlRule rule, Map<GdlVariable, GdlVariable> renamings)
	{
		if (rule.isGround())
		{
			return rule;
		}
		else
		{
			GdlSentence head = renameSentence(rule.getHead(), renamings);

			List<GdlLiteral> body = new ArrayList<GdlLiteral>();
			for (int i = 0; i < rule.arity(); i++)
			{
				body.add(renameLiteral(rule.get(i), renamings));
			}

			return GdlPool.getRule(head, body);
		}
	}

	private GdlSentence renameSentence(GdlSentence sentence, Map<GdlVariable, GdlVariable> renamings)
	{
		if (sentence instanceof GdlProposition)
		{
			return renameProposition((GdlProposition) sentence, renamings);
		}
		else
		{
			return renameRelation((GdlRelation) sentence, renamings);
		}
	}

	private GdlTerm renameTerm(GdlTerm term, Map<GdlVariable, GdlVariable> renamings)
	{
		if (term instanceof GdlConstant)
		{
			return renameConstant((GdlConstant) term, renamings);
		}
		else if (term instanceof GdlVariable)
		{
			return renameVariable((GdlVariable) term, renamings);
		}
		else
		{
			return renameFunction((GdlFunction) term, renamings);
		}
	}

	private GdlVariable renameVariable(GdlVariable variable, Map<GdlVariable, GdlVariable> renamings)
	{
		if (!renamings.containsKey(variable))
		{
			GdlVariable newName = GdlPool.getVariable("?R" + (nextName++));
			renamings.put(variable, newName);
		}

		return renamings.get(variable);
	}

}
