package org.ggp.base.apps.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.server.GameServer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

/**
 * GameServerRunner is a utility program that lets you start up a match
 * directly from the command line. It takes the following arguments:
 *
 * args[0] = tournament name, for storing results
 * args[1] = game key, for loading the game
 * args[2] = start clock, in seconds
 * args[3] = play clock, in seconds
 * args[4,5,6] = host, port, name for player 1
 * args[7,8,9] = host, port, name for player 2
 * etc...
 *
 * @author Evan Cox
 * @author Sam Schreiber
 */
public final class GameServerRunner
{
	public static void main(String[] args) throws IOException, SymbolFormatException, GdlFormatException, InterruptedException, GoalDefinitionException
	{
		// Extract the desired configuration from the command line.
		String tourneyName = args[0];
		String gameKey = args[1];
		Game game = GameRepository.getDefaultRepository().getGame(gameKey);
		int startClock = Integer.valueOf(args[2]);
		int playClock = Integer.valueOf(args[3]);
		if ((args.length - 4) % 3 != 0) {
			throw new RuntimeException("Invalid number of player arguments of the form host/port/name.");
		}
		List<String> hostNames = new ArrayList<String>();
		List<String> playerNames = new ArrayList<String>();
		List<Integer> portNumbers = new ArrayList<Integer>();
		String matchName = tourneyName + "." + gameKey + "." + System.currentTimeMillis();
		for (int i = 4; i < args.length; i += 3) {
			String hostname = args[i];
			Integer port = Integer.valueOf(args[i + 1]);
			String name = args[i + 2];
			hostNames.add(hostname);
			portNumbers.add(port);
			playerNames.add(name);
		}
		int expectedRoles = Role.computeRoles(game.getRules()).size();
		if (hostNames.size() != expectedRoles) {
			throw new RuntimeException("Invalid number of players for game " + gameKey + ": " + hostNames.size() + " vs " + expectedRoles);
		}
		Match match = new Match(matchName, -1, startClock, playClock, game);
		match.setPlayerNamesFromHost(playerNames);

		// Actually run the match, using the desired configuration.
		GameServer server = new GameServer(match, hostNames, portNumbers);
		server.start();
		server.join();

		// Open up the directory for this tournament.
		// Create a "scores" file if none exists.
		File f = new File(tourneyName);
		if (!f.exists()) {
			f.mkdir();
			f = new File(tourneyName + "/scores");
			f.createNewFile();
		}

		// Open up the XML file for this match, and save the match there.
		f = new File(tourneyName + "/" + matchName + ".xml");
		if (f.exists()) f.delete();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write(match.toXML());
		bw.flush();
		bw.close();

		// Open up the JSON file for this match, and save the match there.
		f = new File(tourneyName + "/" + matchName + ".json");
		if (f.exists()) f.delete();
		bw = new BufferedWriter(new FileWriter(f));
		bw.write(match.toJSON());
		bw.flush();
		bw.close();

		// Save the goals in the "/scores" file for the tournament.
		bw = new BufferedWriter(new FileWriter(tourneyName + "/scores", true));
		List<Integer> goals = server.getGoals();
		String goalStr = "";
		String playerStr = "";
		for (int i = 0; i < goals.size(); i++)
		{
			Integer goal = server.getGoals().get(i);
			goalStr += Integer.toString(goal);
			playerStr += playerNames.get(i);
			if (i != goals.size() - 1)
			{
				playerStr += ",";
				goalStr += ",";
			}
		}
		bw.write(playerStr + "=" + goalStr);
		bw.flush();
		bw.close();
	}
}
