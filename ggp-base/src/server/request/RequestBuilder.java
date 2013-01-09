package server.request;

import java.util.List;

import util.gdl.grammar.Gdl;
import util.statemachine.Move;
import util.statemachine.Role;

public final class RequestBuilder
{
	public static String getPlayRequest(String matchId, List<Move> moves)
	{
		if (moves == null) {
			return "( PLAY " + matchId + " NIL )";
		} else {
			StringBuilder sb = new StringBuilder();

			sb.append("( PLAY " + matchId + " (");
			for (Move move : moves)
			{
				sb.append(move.getContents() + " ");
			}
			sb.append(") )");

			return sb.toString();			
		}
	}

	public static String getStartRequest(String matchId, Role role, List<Gdl> description, int startClock, int playClock)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("( START " + matchId + " " + role + " (");
		for (Gdl gdl : description)
		{
			sb.append(gdl + " ");
		}
		sb.append(") " + startClock + " " + playClock + ")");

		return sb.toString();
	}

	public static String getStopRequest(String matchId, List<Move> moves)
	{
		if (moves == null) {
			return "( STOP " + matchId + " NIL )";
		} else {
			StringBuilder sb = new StringBuilder();
	
			sb.append("( STOP " + matchId + " (");
			for (Move move : moves)
			{
				sb.append(move.getContents() + " ");
			}
			sb.append(") )");
	
			return sb.toString();
		}
	}
}