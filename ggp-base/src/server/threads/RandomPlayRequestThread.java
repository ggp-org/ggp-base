package server.threads;

import java.util.List;
import java.util.Random;

import util.statemachine.Move;

public final class RandomPlayRequestThread extends PlayRequestThread
{
	private Move move;

	public RandomPlayRequestThread(List<Move> legalMoves)
	{
		super(null, null, null, null, null, null, 0, null, false);
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