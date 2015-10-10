package org.ggp.base.apps.research;


/**
 * Histogram is a way of visualizing an unordered collection with repeated
 * elements, by mapping each element to the number of times that it appears.
 * It is an aggregation that maps keys to counts.
 *
 * @author Sam Schreiber
 */
public final class Histogram extends Aggregation<Counter>
{
	public void add(String key) {
		if (!containsEntry(key)) {
			createEntry(key, new Counter());
		}
		getEntryData(key).addValue(1);
	}
}
