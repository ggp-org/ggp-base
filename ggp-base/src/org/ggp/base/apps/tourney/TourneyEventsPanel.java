package org.ggp.base.apps.tourney;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.ggp.base.player.gamer.event.GamerCompletedMatchEvent;
import org.ggp.base.player.gamer.event.GamerNewMatchEvent;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.ui.table.JZebraTable;


/**
 * TourneyEventsPanel is responsible for displaying information
 * about the matches that Tourney has played (and is currently
 * playing). It observes the currently running match, and renders
 * its information onto a JZebraTable. 
 * 
 * @author Sam Schreiber
 */
@SuppressWarnings("serial")
public final class TourneyEventsPanel extends JPanel implements Observer
{
    private TourneyEvent currentEvent;
    private final JZebraTable matchTable;

    public void setCurrentEvent(TourneyEvent currentEvent) {
        this.currentEvent = currentEvent;
        updateAggregates();
    }

    public TourneyEventsPanel()
    {
        super(new GridBagLayout());

        DefaultTableModel model = new DefaultTableModel();		
        model.addColumn("Game");
        model.addColumn("Clocks");
        model.addColumn("Status");
        model.addColumn("Goal Summary");
        model.addColumn("PC");
        model.addColumn("TO");
        model.addColumn("IM");
        model.addColumn("CE");

        matchTable = new JZebraTable(model)
        {
            @Override
            public boolean isCellEditable(int rowIndex, int colIndex)
            {
                return false;
            }
        };
        matchTable.setShowHorizontalLines(true);
        matchTable.setShowVerticalLines(true);

        setColumnWidths();
        model.addRow(new String[] { "", "", "", "", "", "", "", "", "" });

        this.add(new JScrollPane(matchTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));		
    }

    public void observe(Event event)
    {
        if (event instanceof GamerCompletedMatchEvent)
        {
            observe((GamerCompletedMatchEvent) event);
        }
        else if (event instanceof GamerNewMatchEvent)
        {
            observe((GamerNewMatchEvent) event);
        }
    }

    private void setColumnWidths() {
        matchTable.getColumnModel().getColumn(4).setPreferredWidth(1);
        matchTable.getColumnModel().getColumn(5).setPreferredWidth(1);
        matchTable.getColumnModel().getColumn(6).setPreferredWidth(1);
        matchTable.getColumnModel().getColumn(7).setPreferredWidth(1);		
    }

    private void observe(GamerCompletedMatchEvent event)
    {
        DefaultTableModel model = (DefaultTableModel) matchTable.getModel();
        model.setValueAt(currentEvent.gameName, model.getRowCount() - 1, 0);		
        model.setValueAt("(" + currentEvent.startClock + ", " + currentEvent.playClock + ")", model.getRowCount() - 1, 1);		
        model.setValueAt(currentEvent.theStatus, model.getRowCount() - 1, 2);
        model.setValueAt(currentEvent.getGoalString(), model.getRowCount() - 1, 3);
        model.setValueAt(currentEvent.moveCount, model.getRowCount() - 1, 4);
        model.setValueAt(currentEvent.errorCount_Timeouts, model.getRowCount() - 1, 5);
        model.setValueAt(currentEvent.errorCount_IllegalMoves, model.getRowCount() - 1, 6);
        model.setValueAt(currentEvent.errorCount_ConnectionErrors, model.getRowCount() - 1, 7);
        
        // This is a pretty simple heuristic to estimate which players
        // won a particular match, based on the goal values. Still, it
        // seems to work pretty well.
        if(currentEvent.latestGoals != null) {
            boolean wasTie = true;
            for(int i = 0; i < currentEvent.numPlayers; i++) {
                int nGoal = currentEvent.latestGoals.get(i);
                if(nGoal > 75) {
                    wasTie = false;
                    totalWins.set(i, totalWins.get(i) + 1);
                }
            }
            if(wasTie) {
                totalTies++;
            }
            updateAggregates();
        }
        
        setColumnWidths();
    }

    private void observe(GamerNewMatchEvent event)
    {
        DefaultTableModel model = (DefaultTableModel) matchTable.getModel();
        model.addRow(new String[] { "", "", "", "", "", "", "", "", "" });

        setColumnWidths();
    }
    
    private List<Integer> totalWins = null;
    private int totalTies = 0;
    private void updateAggregates() {
        if(totalWins == null) {
            totalWins = new ArrayList<Integer>();
            for(int i = 0; i < currentEvent.numPlayers; i++)
                totalWins.add(0);
        }
        
        DefaultTableModel model = (DefaultTableModel) matchTable.getModel();
        for(int i = 0; i < currentEvent.numPlayers; i++) {
            if(i < 4) {
                model.setValueAt("Player " + (i+1) + " Wins: " + totalWins.get(i), 0, i);
            } else {
                model.setValueAt("P" + (i+1) + " Wins: " + totalWins.get(i), 0, i);
            }
        }
        model.setValueAt("Ties: " + totalTies, 0, currentEvent.numPlayers);
        setColumnWidths();
    }
}
