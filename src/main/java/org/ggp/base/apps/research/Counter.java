package org.ggp.base.apps.research;

/**
 * Counter is a convenience class for tracking a count.
 *
 * @author Sam Schreiber
 */
public final class Counter implements Comparable<Counter>
{
	private double totalValue = 0;

	public void addValue(double value) {
		totalValue += value;
	}

	public double getValue() {
		return totalValue;
	}

	@Override
	public String toString() {
		return "" + ((int)(getValue() * 1000.0)/1000.0);
	}

	@Override
	public int compareTo(Counter arg0) {
		return (int)Math.signum(getValue() - ((Counter)arg0).getValue());
	}
}
