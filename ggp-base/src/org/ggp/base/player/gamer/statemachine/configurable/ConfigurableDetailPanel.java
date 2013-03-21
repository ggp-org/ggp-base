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
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.ui.table.JZebraTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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
	
	public ConfigurableDetailPanel() {
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Available Moves");
		model.addColumn("Computation Time");
		model.addColumn("Selected Move");

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
        final TimeSeriesCollection memory = new TimeSeriesCollection();
        memory.addSeries(memUsage);
        memory.addSeries(memTotal);
        JFreeChart memChart = ChartFactory.createTimeSeriesChart(null, null, "Megabytes", memory, true, true, false);
        memChart.setBackgroundPaint(getBackground());
        ChartPanel memChartPanel = new ChartPanel(memChart);
        memChartPanel.setPreferredSize(new Dimension(500, 250));
        sidePanel.add(memChartPanel);

        counters = new HashSet<Counter>();
        countersCollection = new TimeSeriesCollection();
        JFreeChart counterChart = ChartFactory.createTimeSeriesChart(null, null, "Count per 100ms", countersCollection, true, true, false);
        counterChart.getXYPlot().getRangeAxis().setAutoRangeMinimumSize(1.0);
        counterChart.setBackgroundPaint(getBackground());        
        ChartPanel counterChartPanel = new ChartPanel(counterChart);
        counterChartPanel.setPreferredSize(new Dimension(500, 250));
        sidePanel.add(counterChartPanel);
        
        new AddDataPointThread().start();		
		
		this.add(new JScrollPane(moveTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(sidePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(new JButton(resetButtonMethod()), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
	}

	public void observe(Event event) {
		if (event instanceof GamerNewMatchEvent) {
			observe((GamerNewMatchEvent) event);
		} else if (event instanceof GamerSelectedMoveEvent) {
			observe((GamerSelectedMoveEvent) event);
		}
	}

	private void observe(GamerNewMatchEvent event) {
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.setRowCount(0);
	}

	private void observe(GamerSelectedMoveEvent event) {
		String availableMoves = Integer.toString(event.getMoves().size());
		String computationTime = Long.toString(event.getTime()) + " ms";
		String move = event.getSelection().toString();

		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.addRow(new String[] { availableMoves, computationTime, move });
	}
	
	public Counter addCounter(String name) {
		Counter c = new Counter(name);
		counters.add(c);
		countersCollection.addSeries(c.getTimeSeries());
		return c;
	}
	
	class Counter {
		private double value;
		private TimeSeries series;
		public Counter(String name) {
			value = 0;
			series = new TimeSeries(name);
		}
		public void increment(double by) {
			value += by;
		}
		public TimeSeries getTimeSeries() {
			return series;
		}
		public void aggregate() {
			series.add(new Millisecond(new Date()), value);
			value = 0;
		}
		public void clear() {
			series.clear();
		}
	}
	
	private class AddDataPointThread extends Thread {
		public void run() {
			while (true) {
				memUsage.add(new Millisecond(new Date()), Runtime.getRuntime().totalMemory() / (1024*1024));
				memTotal.add(new Millisecond(new Date()), Runtime.getRuntime().maxMemory() / (1024*1024));
				for (Counter c : counters) {
					c.aggregate();
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