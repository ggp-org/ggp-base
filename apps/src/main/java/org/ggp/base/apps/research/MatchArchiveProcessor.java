package org.ggp.base.apps.research;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * MatchArchiveProcessor is a utility program that reads in serialized
 * match archives, like those available at GGP.org/researchers, and runs
 * some computation on every match in the archive.
 *
 * This can be used to do many useful tasks, like counting the number of
 * matches that satisfy certain properties, totaling up averages across the
 * set of all matches, and computing statistics and player ratings.
 *
 * Currently this computes three interesting example aggregations:
 *   - the frequency of wins for the second player, broken down by game
 *   - a histogram of how often each game is played
 *   - the average length of a nine-board tic-tac-toe match
 *
 * @author Sam Schreiber
 */
public final class MatchArchiveProcessor
{
	// Set this to the path of the downloaded match archive file.
	public static final File ARCHIVE_FILE = new File(new File(new File(new File(System.getProperty("user.home")), "matchArchive"), "data"), "allMatches");

	public static void main(String[] args) throws IOException, JSONException
	{
		AggregateData data = new AggregateData();

		String line;
		int nCount = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ARCHIVE_FILE), Charset.forName("UTF-8")));
		while ((line = br.readLine()) != null) {
			JSONObject entryJSON = new JSONObject(line);
			String url = entryJSON.getString("url");
			JSONObject matchJSON = entryJSON.getJSONObject("data");
			processMatch(url, matchJSON, data);
			nCount++;
			if (nCount % 1000 == 0) {
				System.out.println("Processed " + nCount + " matches.");
			}
		}
		br.close();

		System.out.println("Second player win frequency:\n" + data.secondPlayerWinFrequency.toString());
		System.out.println("Game histogram:\n" + data.gameHistogram.toString());
		System.out.println("average matchLengthsFor9xTTT = " + data.matchLengthsFor9xTTT.toString());
	}

	// This class stores all of the data that needs to be aggregated between
	// individual matches. It will include things like histograms, counters,
	// weighted averages, etc. If you're adding a new aggregation, you'll likely
	// want to add a new data structure here to keep track of the aggregate data
	// between matches.
	static class AggregateData {
		public Histogram gameHistogram = new Histogram();
		public WeightedAverage matchLengthsFor9xTTT = new WeightedAverage();
		public FrequencyTable secondPlayerWinFrequency = new FrequencyTable();
	}

	// This method determines how each individual match is processed.
	// Right now it aggregates data about how often individual games are played,
	// how often the second player wins a match, and how long the average match
	// of nine-board tic-tac-toe takes. If you want to add more aggregations, this
	// is the place to do it.
	private static void processMatch(String theURL, JSONObject matchJSON, AggregateData data) {
		try {
			String gameURL = matchJSON.getString("gameMetaURL");
			// Add a data point to the histogram of how often games are used
			data.gameHistogram.add(gameURL);
			// And for completed signed matches...
			if (matchJSON.has("isCompleted") && matchJSON.getBoolean("isCompleted") && matchJSON.has("matchHostPK")) {
				// Add a data point to the average length of 9xTTT matches, if it's a 9xTTT match
				if (gameURL.startsWith("http://games.ggp.org/base/games/nineBoardTicTacToe/")) {
					data.matchLengthsFor9xTTT.addValue(matchJSON.getJSONArray("states").length());
				}
				// Add a data point to the frequency of second player wins, if this is a match that
				// has a second player and recorded goal values.
				if (matchJSON.has("goalValues") && matchJSON.getJSONArray("goalValues").length() > 1) {
					boolean secondPlayerWon = true;
					JSONArray goalValues = matchJSON.getJSONArray("goalValues");
					for (int i = 0; i < goalValues.length(); i++) {
						if (i == 1) continue;
						if (goalValues.getInt(i) >= goalValues.getInt(1)) {
							secondPlayerWon = false;
						}
					}
					data.secondPlayerWinFrequency.add(gameURL, secondPlayerWon ? 1 : 0);
				}
			}
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}
}
