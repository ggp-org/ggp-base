package player.request.factory;

import java.util.ArrayList;
import java.util.List;

import player.gamer.Gamer;
import player.request.factory.exceptions.RequestFormatException;
import player.request.grammar.AbortRequest;
import player.request.grammar.PingRequest;
import player.request.grammar.PlayRequest;
import player.request.grammar.Request;
import player.request.grammar.StartRequest;
import player.request.grammar.StopRequest;
import util.game.Game;
import util.gdl.factory.GdlFactory;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.symbol.factory.SymbolFactory;
import util.symbol.grammar.Symbol;
import util.symbol.grammar.SymbolAtom;
import util.symbol.grammar.SymbolList;

public final class RequestFactory
{
	public Request create(Gamer gamer, String source) throws RequestFormatException
	{
		try
		{
			SymbolList list = (SymbolList) SymbolFactory.create(source);
			SymbolAtom head = (SymbolAtom) list.get(0);

			String type = head.getValue().toLowerCase();
			if (type.equals("play"))
			{
				return createPlay(gamer, list);
			}
			else if (type.equals("start"))
			{
				return createStart(gamer, list);
			}
			else if (type.equals("stop"))
			{
				return createStop(gamer, list);
			}
			else if (type.equals("abort"))
			{
			    return createAbort(gamer, list);
			}
			else if (type.equals("ping"))
			{
			    return createPing(gamer, list);
			}
			else
			{
				throw new IllegalArgumentException("Unrecognized request type!");
			}
		}
		catch (Exception e)
		{
			throw new RequestFormatException(source, e);
		}
	}

	private PlayRequest createPlay(Gamer gamer, SymbolList list) throws GdlFormatException
	{
		if (list.size() != 3)
		{
			throw new IllegalArgumentException("Expected exactly 2 arguments!");
		}

		SymbolAtom arg1 = (SymbolAtom) list.get(1);
		Symbol arg2 = list.get(2);

		String matchId = arg1.getValue();
		List<GdlSentence> moves = parseMoves(arg2);

		return new PlayRequest(gamer, matchId, moves);
	}

	private StartRequest createStart(Gamer gamer, SymbolList list) throws GdlFormatException
	{
		if (list.size() < 6)
		{
			throw new IllegalArgumentException("Expected at least 5 arguments!");
		}

		SymbolAtom arg1 = (SymbolAtom) list.get(1);
		SymbolAtom arg2 = (SymbolAtom) list.get(2);
		SymbolList arg3 = (SymbolList) list.get(3);
		SymbolAtom arg4 = (SymbolAtom) list.get(4);
		SymbolAtom arg5 = (SymbolAtom) list.get(5);

		String matchId = arg1.getValue();
		GdlProposition roleName = (GdlProposition) GdlFactory.create(arg2);
		List<Gdl> theRules = parseDescription(arg3);
		int startClock = Integer.valueOf(arg4.getValue());
		int playClock = Integer.valueOf(arg5.getValue());

		// TODO: There may be more than five arguments. These may be worth
		// parsing, once we find a meaningful way to handle them. They aren't
		// yet standardized, but, for example, one might be the URL of an XSL
		// stylesheet for visualizing a state of the game, or the URL for the
		// game on a repository server.

		Game theReceivedGame = Game.createEphemeralGame(theRules);
		return new StartRequest(gamer, matchId, roleName, theReceivedGame, startClock, playClock);
	}

	private StopRequest createStop(Gamer gamer, SymbolList list) throws GdlFormatException
	{
		if (list.size() != 3)
		{
			throw new IllegalArgumentException("Expected exactly 2 arguments!");
		}

		SymbolAtom arg1 = (SymbolAtom) list.get(1);
		Symbol arg2 = list.get(2);

		String matchId = arg1.getValue();
		List<GdlSentence> moves = parseMoves(arg2);

		return new StopRequest(gamer, matchId, moves);
	}
	
    private AbortRequest createAbort(Gamer gamer, SymbolList list) throws GdlFormatException
    {
        if (list.size() != 2)
        {
            throw new IllegalArgumentException("Expected exactly 2 arguments!");
        }

        SymbolAtom arg1 = (SymbolAtom) list.get(1);
        String matchId = arg1.getValue();

        return new AbortRequest(gamer, matchId);
    }	
    
    private PingRequest createPing(Gamer gamer, SymbolList list) throws GdlFormatException
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Expected exactly 1 argument!");
        }

        return new PingRequest(gamer);
    }       

	private List<Gdl> parseDescription(SymbolList list) throws GdlFormatException
	{
		List<Gdl> description = new ArrayList<Gdl>();
		for (int i = 0; i < list.size(); i++)
		{
			description.add(GdlFactory.create(list.get(i)));
		}

		return description;
	}

	private List<GdlSentence> parseMoves(Symbol symbol) throws GdlFormatException
	{
		if (symbol instanceof SymbolAtom)
		{
			return null;
		}
		else
		{
			List<GdlSentence> moves = new ArrayList<GdlSentence>();
			SymbolList list = (SymbolList) symbol;

			for (int i = 0; i < list.size(); i++)
			{
				moves.add((GdlSentence) GdlFactory.create(list.get(i)));
			}

			return moves;
		}
	}
}