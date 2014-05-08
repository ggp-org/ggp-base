package org.ggp.base.apps.research;

/**
 * WeightedAverage is a convenience class for tracking a weighted average.
 *
 * @author Sam Schreiber
 */
public final class WeightedAverage implements Comparable<WeightedAverage>
{
	private double totalValue = 0, totalWeight = 0;

	public void addValue(double value) {
		addValue(value, 1.0);
	}

	public void addValue(double value, double weight) {
		totalValue += value;
		totalWeight += weight;
	}

	public double getValue() {
		return totalValue / totalWeight;
	}

	public double getWeight() {
		return totalWeight;
	}

	@Override
	public String toString() {
		return "" + ((int)(getValue() * 1000.0)/1000.0) + " [" + getWeight() + "]";
	}

	@Override
	public int compareTo(WeightedAverage arg0) {
		return (int)Math.signum(getValue() - ((WeightedAverage)arg0).getValue());
	}
}
