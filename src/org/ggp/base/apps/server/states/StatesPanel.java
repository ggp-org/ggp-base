package org.ggp.base.apps.server.states;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.ggp.base.server.event.ServerNewGameStateEvent;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.statemachine.MachineState;


@SuppressWarnings("serial")
public class StatesPanel extends JPanel implements Observer {
	private JTabbedPane tabs = new JTabbedPane();
	
	public StatesPanel()
	{		
		this.add(tabs);		
	}

	private int stepCount = 1;
	public void observe(Event event) {
		if (event instanceof ServerNewGameStateEvent)
		{
	        MachineState s = ((ServerNewGameStateEvent)event).getState();
	        // TODO: Perhaps this should run in a separate thread, as in the
	        // VisualizationPanel, in case these states are very large.
	        JPanel statePanel = new JPanel();
	        List<String> sentences = new ArrayList<String>();
	        for(GdlSentence sentence : s.getContents())
	        	sentences.add(sentence.toString());
	        //The list of sentences is more useful when sorted alphabetically.
	        Collections.sort(sentences);
	        StringBuffer sentencesList = new StringBuffer();
	        for(String sentence : sentences)
	        	sentencesList.append(sentence).append("\n");
	        JTextArea statesTextArea = new JTextArea(sentencesList.toString());
	        statesTextArea.setEditable(false);
	        JScrollPane scrollPane = new JScrollPane(statesTextArea);
	        scrollPane.setPreferredSize(new Dimension(400, 500));
	        statePanel.add(scrollPane);
	        
	        // Add the panel as a new tab
	        // Reusing the VisualizationPanel code, to make it easier in case this gets
	        // moved off into a new thread
	        int stepNum = stepCount;
	        stepCount++;
	        boolean atEnd = (tabs.getSelectedIndex() == tabs.getTabCount()-1);

	        for(int i = tabs.getTabCount(); i < stepNum; i++)
	        	tabs.add(new Integer(i+1).toString(), new JPanel());
	        tabs.setComponentAt(stepNum-1, statePanel);
	        tabs.setTitleAt(stepNum-1, new Integer(stepNum).toString());

	        if(atEnd) {             
	        	tabs.setSelectedIndex(tabs.getTabCount()-1);
	        }
		}
	}

}
