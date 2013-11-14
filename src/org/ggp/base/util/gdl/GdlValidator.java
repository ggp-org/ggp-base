package org.ggp.base.util.gdl;

import org.ggp.base.util.symbol.grammar.Symbol;
import org.ggp.base.util.symbol.grammar.SymbolAtom;
import org.ggp.base.util.symbol.grammar.SymbolList;

/**
 * The GdlValidator class implements Gdl validation for the GdlFactory class.
 * Its purpose is to validate whether or not a Symbol can be transformed into a
 * Gdl expression without error.
 */
public final class GdlValidator
{

	/**
	 * Validates whether a Symbol can be transformed into a Gdl expression
	 * without error using the following process:
	 * <ol>
	 * <li>Returns true if the Symbol is a SymbolAtom. Otherwise, treats the
	 * Symbol as a SymbolList.</li>
	 * <li>Checks that the SymbolList contains no sub-elements that are
	 * SymbolLists which do not begin with a SymbolAtom.</li>
	 * <li>Checks that neither the SymbolList nor its sub-elements contain the
	 * deprecated 'or' keyword.</li>
	 * </ol>
	 * Note that as implemented, this method is incomplete: it only verifies a
	 * subset of the correctness properties of well-formed Gdl. A more thorough
	 * implementation is advisable.
	 *
	 * @param symbol
	 *            The Symbol to validate.
	 * @return True if the Symbol passes validation; false otherwise.
	 */
	public boolean validate(Symbol symbol)
	{
		if ( symbol instanceof SymbolAtom )
		{
			return true;
		}
		else if ( containsAnonymousList(symbol) )
		{
			return false;
		}
		else if ( containsOr(symbol) )
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * A recursive method that checks whether a Symbol contains SymbolList that
	 * does not begin with a SymbolAtom.
	 *
	 * @param symbol
	 *            The Symbol to validate.
	 * @return True if the Symbol passes validation; false otherwise.
	 */
	private boolean containsAnonymousList(Symbol symbol)
	{
		if ( symbol instanceof SymbolAtom )
		{
			return false;
		}
		else
		{
			if ( symbol instanceof SymbolList )
			{
				return true;
			}
			else
			{
				for ( int i = 1; i < ((SymbolList)symbol).size(); i++ )
				{
					if ( containsAnonymousList(((SymbolList)symbol).get(i)) )
					{
						return true;
					}
				}

				return false;
			}
		}
	}

	/**
	 * A recursive method that checks whether a Symbol contains the deprecated
	 * 'or' keyword.
	 *
	 * @param symbol
	 *            The Symbol to validate.
	 * @return True if the Symbol passes validation; false otherwise.
	 */
	private boolean containsOr(Symbol symbol)
	{
		if ( symbol instanceof SymbolAtom )
		{
			return false;
		}
		else
		{
			if ( symbol.toString().toLowerCase().equals("or") )
			{
				return true;
			}
			else if ( symbol instanceof SymbolList )
			{
				for ( int i = 1; i < ((SymbolList)symbol).size(); i++ )
				{
					if ( containsOr(((SymbolList)symbol).get(i)) )
					{
						return true;
					}
				}
			}
		}
		return false;
	}

}
