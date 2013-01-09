package server.threads;

import java.util.List;
import java.util.Random;

import util.match.Match;
import util.statemachine.Move;

public final class RandomPlayRequestThread extends PlayRequestThread
{
	private Move move;

	public RandomPlayRequestThread(Match match, List<Move> legalMoves)
	{
		super(null, match, null, legalMoves, null, null, 0, null, true);
		move = legalMoves.get(new Random().nextInt(legalMoves.size()));
	}

	@Override
	public Move getMove()
	{
		return move;
	}

	@Override
	public void run()
	{
		;
	}
}