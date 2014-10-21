package org.ggp.base.apps.research;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Aggregation is a way of storing data about an unordered collection, mapping
 * each element to an associated data object, and then visualizing the data by
 * sorting the elements by their associated data and printing out the sorted list.
 * The data objects must be comparable to each other, so they can be sorted.
 *
 * @author Sam Schreiber
 */
public abstract class Aggregation<T extends Comparable<T>>
{
	final private Map<String, T> entryData = new HashMap<String, T>();

	boolean containsEntry(String key) {
		return entryData.containsKey(key);
	}

	void createEntry(String key, T data) {
		entryData.put(key, data);
	}

	T getEntryData(String key) {
		return entryData.get(key);
	}

	private final class EntryComparator implements Comparator<Map.Entry<String,T>> {
		@Override
		public int compare(Map.Entry<String,T> a, Map.Entry<String,T> b) {
			return a.getValue().compareTo(b.getValue());
		}

	}

	@Override
	public String toString() {
		int nMaxLength = 0;
		StringBuilder theStringRep = new StringBuilder();
		TreeSet<Map.Entry<String,T>> theEntries = new TreeSet<Map.Entry<String,T>>(new EntryComparator());
		theEntries.addAll(entryData.entrySet());
		for (Map.Entry<String,T> entry : theEntries) {
			nMaxLength = Math.max(nMaxLength, entry.getKey().length());
		}
		for (Map.Entry<String,T> entry : theEntries) {
			theStringRep.append(String.format("%1$-" + (nMaxLength + 5) + "s", entry.getKey()) + entry.getValue() + "\n");
		}
		return theStringRep.toString();
	}
}
