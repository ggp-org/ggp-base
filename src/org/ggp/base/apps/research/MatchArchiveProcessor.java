package org.ggp.base.apps.research;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * MatchArchiveProcessor is a utility program that reads in serialized
 * match archives, like those available at GGP.org/researchers, and runs
 * some computation on every match in the archive.
 *
 * This can be used to do many useful tasks, including:
 *   - counting the number of matches run by different hosting systems
 *   - counting the number of match transcripts that are well-formed
 *   - computing simple statistics and player ratings
 *
 * Currently it aggregates data into a histogram, and then displays that
 * histogram when all of the matches have been processed. This isn't the
 * only way in which match archives can be processed, but it works as an
 * example. Currently the data being aggregated is the number of matches
 * played of each distinct game.
 *
 * @author Sam Schreiber
 */
public final class MatchArchiveProcessor
{
	// Set this to the path of the downloaded match archive file.
	public static final File ARCHIVE_FILE = new File(new File(new File(new File(System.getProperty("user.home")), "matchArchive"), "data"), "allMatches");

	public static void main(String[] args) throws IOException, JSONException
	{
		Histogram theAggregator = new Histogram();

		String line;
		int nCount = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ARCHIVE_FILE), Charset.forName("UTF-8")));
		while ((line = br.readLine()) != null) {
			JSONObject entryJSON = new JSONObject(line);
			String url = entryJSON.getString("url");
			JSONObject matchJSON = entryJSON.getJSONObject("data");
			processMatch(url, matchJSON, theAggregator);
			nCount++;
			if (nCount % 1000 == 0) {
				System.out.println("Processed " + nCount + " matches.");
			}
		}
		br.close();

		System.out.println(theAggregator.toString());
	}

	// This method determines how each individual match is processed.
	// Right now it just extracts the game URL and adds it to a histogram.
	// If you want to perform some other type of processing, it should be
	// implemented here.
	private static void processMatch(String theURL, JSONObject matchJSON, Histogram theAggregator) {
		try {
			String gameURL = matchJSON.getString("gameMetaURL");
			theAggregator.add(gameURL);
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}
}
