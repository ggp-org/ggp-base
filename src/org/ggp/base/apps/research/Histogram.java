package org.ggp.base.apps.research;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Histogram is a way of visualizing an unordered collection with repeated
 * elements, by mapping each element to the number of times that it appears.
 *
 * @author Sam Schreiber
 */
public final class Histogram
{
	final private Map<String, Integer> entryCounts = new HashMap<String, Integer>();

	public void add(String key) {
		if (entryCounts.containsKey(key)) {
			entryCounts.put(key, entryCounts.get(key) + 1);
		} else {
			entryCounts.put(key, 1);
		}
	}

	private static final class EntryComparator implements Comparator<Map.Entry<String,Integer>> {
		@Override
		public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) {
			return a.getValue() - b.getValue();
		}

	}

	@Override
	public String toString() {
		int nMaxLength = 0;
		StringBuilder theStringRep = new StringBuilder();
		TreeSet<Map.Entry<String,Integer>> theEntries = new TreeSet<Map.Entry<String,Integer>>(new EntryComparator());
		theEntries.addAll(entryCounts.entrySet());
		for (Map.Entry<String,Integer> entry : theEntries) {
			nMaxLength = Math.max(nMaxLength, entry.getKey().length());
		}
		for (Map.Entry<String,Integer> entry : theEntries) {
			theStringRep.append(String.format("%1$-" + (nMaxLength + 5) + "s", entry.getKey()) + entry.getValue() + "\n");
		}
		return theStringRep.toString();
	}
}
