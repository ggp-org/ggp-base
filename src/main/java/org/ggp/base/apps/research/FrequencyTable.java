package org.ggp.base.apps.research;

/**
 * FrequencyTable is an aggregation that maps keys to weighted averages.
 *
 * @author Sam Schreiber
 */
public final class FrequencyTable extends Aggregation<WeightedAverage>
{
	public void add(String key, double value) {
		if (!containsEntry(key)) {
			createEntry(key, new WeightedAverage());
		}
		getEntryData(key).addValue(value);
	}
}