package org.ggp.base.player.gamer.statemachine.configurable;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.player.gamer.event.GamerNewMatchEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.ui.table.JZebraTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

@SuppressWarnings("serial")
public class ConfigurableDetailPanel extends DetailPanel {
	private final JZebraTable moveTable;

	private final TimeSeries memUsage;
	private final TimeSeries memTotal;
	
	final Set<Counter> counters;
	final TimeSeriesCollection countersCollection;
	final TimeSeriesCollection scoreCountersCollection;
	
	public ConfigurableDetailPanel() {
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Step");
		model.addColumn("My Move");
		model.addColumn("Time spent");
		model.addColumn("Out of time?");

		moveTable = new JZebraTable(model) {
			@Override
			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false;
			}
		};
		moveTable.setShowHorizontalLines(true);
		moveTable.setShowVerticalLines(true);

		JPanel sidePanel = new JPanel();
		
        memUsage = new TimeSeries("Used Memory");
        memTotal = new TimeSeries("Total Memory");
        memUsage.setMaximumItemCount(36000);
        memTotal.setMaximumItemCount(36000);
        final TimeSeriesCollection memory = new TimeSeriesCollection();
        memory.addSeries(memUsage);
        memory.addSeries(memTotal);
        JFreeChart memChart = ChartFactory.createTimeSeriesChart(null, null, "Megabytes", memory, true, true, false);
        memChart.setBackgroundPaint(getBackground());
        ChartPanel memChartPanel = new ChartPanel(memChart);
        memChartPanel.setPreferredSize(new Dimension(500, 175));
        sidePanel.add(memChartPanel);

        counters = new HashSet<Counter>();
        countersCollection = new TimeSeriesCollection();
        JFreeChart counterChart = ChartFactory.createTimeSeriesChart(null, null, null, countersCollection, true, true, false);
        counterChart.getXYPlot().setRangeAxis(new LogarithmicAxis("Count per 100ms"));
        counterChart.getXYPlot().getRangeAxis().setAutoRangeMinimumSize(1.0);
        counterChart.setBackgroundPaint(getBackground());
        ChartPanel counterChartPanel = new ChartPanel(counterChart);
        counterChartPanel.setPreferredSize(new Dimension(500, 175));
        sidePanel.add(counterChartPanel);
        
        scoreCountersCollection = new TimeSeriesCollection();
        JFreeChart scoreCounterChart = ChartFactory.createTimeSeriesChart(null, null, "Score", scoreCountersCollection, true, true, false);
        scoreCounterChart.getXYPlot().getRangeAxis().setRange(0, 100);
        scoreCounterChart.setBackgroundPaint(getBackground());
        ChartPanel scoreCounterChartPanel = new ChartPanel(scoreCounterChart);
        scoreCounterChartPanel.setPreferredSize(new Dimension(500, 175));
        sidePanel.add(scoreCounterChartPanel);        
        
		this.add(new JScrollPane(moveTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(sidePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(new JButton(resetButtonMethod()), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
	}
	
	private boolean startedAdding = false;
	public void beginAddingDataPoints() {
		if (!startedAdding) {
			new AddDataPointThread().start();
			startedAdding = true;
		}
	}

	public void observe(Event event) {
		if (event instanceof GamerNewMatchEvent) {
			observe((GamerNewMatchEvent) event);
		}
	}

	private void observe(GamerNewMatchEvent event) {
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.setRowCount(0);
	}
	
	public void addObservation(int step, Move move, long timeSpent, boolean ranOut) {
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.addRow(new String[] { ""+step, move.toString(), ""+timeSpent+" ms", ranOut ? "<html><font color=red>Yes</font></html>" : "No" });
	}

	abstract class Counter {
		private TimeSeries series;
		public Counter(String name, boolean forScore) {			
			series = new TimeSeries(name);
			series.setMaximumItemCount(36000);
			counters.add(this);
			if (forScore) {
				scoreCountersCollection.addSeries(series);
			} else {
				countersCollection.addSeries(series);
			}		
		}
		public TimeSeries getTimeSeries() {
			return series;
		}
		public void consolidate() {
			series.add(new Millisecond(new Date()), getValue());
		}
		protected abstract Double getValue();
		public void clear() {
			series.clear();
		}
	}
	
	class AggregatingCounter extends Counter {
		private double value;
		public AggregatingCounter(String name, boolean forScore) {
			super(name, forScore);
			value = 0;
		}
		public void increment(double by) {
			value += by;
		}
		@Override
		protected Double getValue() {
			double theValue = value;
			value = 0;
			return (theValue > 0) ? theValue : null;
		}
	}
	
	class FixedCounter extends Counter {
		private Double value;
		public FixedCounter(String name, boolean forScore) {
			super(name, forScore);
			value = null;
		}
		public void set(double to) {
			value = to;
		}
		@Override
		protected Double getValue() {
			return value;
		}
	}
	
	private class AddDataPointThread extends Thread {
		public void run() {
			while (true) {
				memUsage.add(new Millisecond(new Date()), Runtime.getRuntime().totalMemory() / (1024*1024));
				memTotal.add(new Millisecond(new Date()), Runtime.getRuntime().maxMemory() / (1024*1024));
				for (Counter c : counters) {
					c.consolidate();
				}
				repaint();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	AbstractAction resetButtonMethod() {
		return new AbstractAction("Reset Time Series") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent evt) {
				memUsage.clear();
				memTotal.clear();
				for (Counter c : counters) {
					c.clear();
				}
			}
		};
	}
}