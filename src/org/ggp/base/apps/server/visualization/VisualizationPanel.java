package org.ggp.base.apps.server.visualization;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.ggp.base.server.event.ServerCompletedMatchEvent;
import org.ggp.base.server.event.ServerNewGameStateEvent;
import org.ggp.base.server.event.ServerNewMatchEvent;
import org.ggp.base.server.event.ServerTimeEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.ui.timer.JTimerBar;

@SuppressWarnings("serial")
public final class VisualizationPanel extends JPanel implements Observer
{
	private final Game theGame;
	private final VisualizationPanel myThis;
	private JTabbedPane tabs = new JTabbedPane();
	private final JTimerBar timerBar;
	private static final Object tabLock = new Object();

	public VisualizationPanel(Game theGame)
	{
		super(new GridBagLayout());
		this.theGame = theGame;
		this.myThis = this;
		this.timerBar = new JTimerBar();
		this.add(tabs, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));		
		this.add(timerBar, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	private int stepCount = 1;
	public void observe(Event event)
	{
	    if (event instanceof ServerNewGameStateEvent) {
	        MachineState s = ((ServerNewGameStateEvent)event).getState();
	        RenderThread rt = new RenderThread(s, stepCount++);
	        rt.start();
		} else if (event instanceof ServerTimeEvent) {
			timerBar.time(((ServerTimeEvent) event).getTime(), 500);
		} else if (event instanceof ServerCompletedMatchEvent) {
			timerBar.stop();
		} else if (event instanceof ServerNewMatchEvent) {
			MachineState s = ((ServerNewMatchEvent) event).getInitialState();
			RenderThread rt = new RenderThread(s, stepCount);
			rt.start();
		}
	}
	
	private class RenderThread extends Thread {  
	    private final MachineState s;
	    private final int stepNum;
	    
	    public RenderThread(MachineState s, int stepNum) {
	        this.s = s;
	        this.stepNum = stepNum;
	    }
	    
	    @Override
	    public void run()
	    {
	        JPanel newPanel = null;
	        try {
                String XML = Match.renderStateXML(s.getContents());
                String XSL = theGame.getStylesheet();
                if (XSL != null) {
                    newPanel = new VizContainerPanel(XML, XSL, myThis);
                }
	        } catch(Exception ex) {
	            ex.printStackTrace();
	        }
	        
	        if(newPanel != null) {
	            // Add the rendered panel as a new tab
	            synchronized(tabLock) {
	                boolean atEnd = (tabs.getSelectedIndex() == tabs.getTabCount()-1);
	                try {
	                    for(int i = tabs.getTabCount(); i < stepNum; i++)
	                        tabs.add(new Integer(i+1).toString(), new JPanel());
	                    tabs.setComponentAt(stepNum-1, newPanel);
	                    tabs.setTitleAt(stepNum-1, new Integer(stepNum).toString());

	                    if(atEnd) {             
	                        tabs.setSelectedIndex(tabs.getTabCount()-1);
	                    }
	                } catch(Exception ex) {
	                    System.err.println("Adding rendered visualization panel failed for: " + theGame.getKey());
	                }
	            }
	        }
	    }
	}

	// Simple test that loads the nineBoardTicTacToe game and visualizes
	// a randomly-played match, to demonstrate that visualization works.
	public static void main(String args[]) {
        JFrame frame = new JFrame("Visualization Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Game theGame = GameRepository.getDefaultRepository().getGame("nineBoardTicTacToe");
        VisualizationPanel theVisual = new VisualizationPanel(theGame);
        frame.setPreferredSize(new Dimension(1200, 900));
        frame.getContentPane().add(theVisual);
        frame.pack();
        frame.setVisible(true);
        
        StateMachine theMachine = new CachedStateMachine(new ProverStateMachine());        
        theMachine.initialize(theGame.getRules());
        try {
            MachineState theCurrentState = theMachine.getInitialState();            
            do {
                theVisual.observe(new ServerNewGameStateEvent(theCurrentState));
                theCurrentState = theMachine.getRandomNextState(theCurrentState);
                Thread.sleep(250);
                System.out.println("State: " + theCurrentState);
            } while(!theMachine.isTerminal(theCurrentState));
            theVisual.observe(new ServerNewGameStateEvent(theCurrentState));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}