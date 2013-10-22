package org.ggp.base.util.gdl.factory;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
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
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.Symbol;
import org.ggp.base.util.symbol.grammar.SymbolAtom;
import org.ggp.base.util.symbol.grammar.SymbolList;


public final class GdlFactory
{

	public static Gdl create(String string) throws GdlFormatException, SymbolFormatException
	{
		return create(SymbolFactory.create(string));
	}

	public static Gdl create(Symbol symbol) throws GdlFormatException
	{
		try
		{
			return createGdl(symbol);
		}
		catch (Exception e)
		{
			createGdl(symbol);
			throw new GdlFormatException(symbol);
		}
	}

	private static GdlConstant createConstant(SymbolAtom atom)
	{
        return GdlPool.getConstant(atom.getValue());
	}

	private static GdlDistinct createDistinct(SymbolList list)
	{
		GdlTerm arg1 = createTerm(list.get(1));
		GdlTerm arg2 = createTerm(list.get(2));

		return GdlPool.getDistinct(arg1, arg2);
	}

	private static GdlFunction createFunction(SymbolList list)
	{
		GdlConstant name = createConstant((SymbolAtom) list.get(0));

		List<GdlTerm> body = new ArrayList<GdlTerm>();
		for (int i = 1; i < list.size(); i++)
		{
			body.add(createTerm(list.get(i)));
		}

		return GdlPool.getFunction(name, body);
	}

	private static Gdl createGdl(Symbol symbol)
	{
		if (symbol instanceof SymbolList)
		{
			SymbolList list = (SymbolList) symbol;
			SymbolAtom type = (SymbolAtom) list.get(0);

			if (type.getValue().equals("<="))
			{
				return createRule(list);
			}
		}

		return createSentence(symbol);
	}

	private static GdlLiteral createLiteral(Symbol symbol)
	{
		if (symbol instanceof SymbolList)
		{
			SymbolList list = (SymbolList) symbol;
			SymbolAtom type = (SymbolAtom) list.get(0);

			if (type.getValue().toLowerCase().equals("distinct"))
			{
				return createDistinct(list);
			}
			else if (type.getValue().toLowerCase().equals("not"))
			{
				return createNot(list);
			}
			else if (type.getValue().toLowerCase().equals("or"))
			{
				return createOr(list);
			}
		}

		return createSentence(symbol);
	}

	private static GdlNot createNot(SymbolList list)
	{
		return GdlPool.getNot(createLiteral(list.get(1)));
	}

	private static GdlOr createOr(SymbolList list)
	{
		List<GdlLiteral> disjuncts = new ArrayList<GdlLiteral>();
		for (int i = 1; i < list.size(); i++)
		{
			disjuncts.add(createLiteral(list.get(i)));
		}

		return GdlPool.getOr(disjuncts);
	}

	private static GdlProposition createProposition(SymbolAtom atom)
	{
		return GdlPool.getProposition(createConstant(atom));
	}

	private static GdlRelation createRelation(SymbolList list)
	{
		GdlConstant name = createConstant((SymbolAtom) list.get(0));

		List<GdlTerm> body = new ArrayList<GdlTerm>();
		for (int i = 1; i < list.size(); i++)
		{
			body.add(createTerm(list.get(i)));
		}

		return GdlPool.getRelation(name, body);
	}

	private static GdlRule createRule(SymbolList list)
	{
		GdlSentence head = createSentence(list.get(1));

		List<GdlLiteral> body = new ArrayList<GdlLiteral>();
		for (int i = 2; i < list.size(); i++)
		{
			body.add(createLiteral(list.get(i)));
		}

		return GdlPool.getRule(head, body);
	}

	private static GdlSentence createSentence(Symbol symbol)
	{
		if (symbol instanceof SymbolAtom)
		{
			return createProposition((SymbolAtom) symbol);
		}
		else
		{
			return createRelation((SymbolList) symbol);
		}
	}

	public static GdlTerm createTerm(String string) throws SymbolFormatException
	{
		return createTerm(SymbolFactory.create(string));
	}

	public static GdlTerm createTerm(Symbol symbol)
	{
		if (symbol instanceof SymbolAtom)
		{
			SymbolAtom atom = (SymbolAtom) symbol;
			if (atom.getValue().charAt(0) == '?')
			{
				return createVariable(atom);
			}
			else
			{
				return createConstant(atom);
			}
		}
		else
		{
			return createFunction((SymbolList) symbol);
		}
	}

	private static GdlVariable createVariable(SymbolAtom atom)
	{
		return GdlPool.getVariable(atom.getValue());
	}

}
