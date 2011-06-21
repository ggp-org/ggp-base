package apps.server.visualization;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import apps.common.timer.JTimerBar;

import server.event.ServerCompletedMatchEvent;
import server.event.ServerNewGameStateEvent;
import server.event.ServerNewMatchEvent;
import server.event.ServerTimeEvent;
import util.game.Game;
import util.game.GameRepository;
import util.observer.Event;
import util.observer.Observer;
import util.statemachine.MachineState;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import util.xhtml.GameStateRenderPanel;

@SuppressWarnings("serial")
public final class VisualizationPanel extends JPanel implements Observer
{
	private final Game theGame;
	private final VisualizationPanel myThis;
	private JTabbedPane tabs = new JTabbedPane();
	private final JTimerBar timerBar;

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
	    if (event instanceof ServerNewGameStateEvent)
		{
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
	            // NOTE: This controls whether we use the legacy local stylesheet
	            // visualizations or the newer web-hosted visualizations. Ultimately
	            // we want to convert the legacy stylesheets to web-hosted versions
	            // and then phase out the legacy local system. For now, we will try
	            // to use a web-hosted visualization, and fall back to a local one
	            // if the web-hosted visualization isn't available.
	        	
	        	// The above does not seem to describe the current behavior, and I'm not
	        	// sure that behavior would be preferred in all cases. If I am working on
	        	// a new stylesheet for an existing game on my own computer, I would
	        	// prefer that it use the local stylesheet. On the other hand, if I were
	        	// working on the rulesheet locally, I would want it to use the web-hosted
	        	// visualization if I didn't have anything to override it with. My
	        	// (unimplemented) recommendation: Default to web-hosted visualizations for
	        	// web-hosted games and locally-hosted visualizations for locally-hosted
	        	// games. Use the other if the default is not present. I believe this is
	        	// orthogonal to the issue of what the stylesheets should look like, which
	        	// I would like to see unified as what the web-hosted versions are now. -A.L.
	            if (theGame.getStylesheet() == null) {
	                String XML = s.toMatchXML();
	                String XSL = GameStateRenderPanel.getXSLfromFile(theGame.getKey()); 
	                newPanel = new VizContainerPanel(XML, XSL, true, myThis);
	            } else {
                    String XML = s.toXML();
                    String XSL = theGame.getStylesheet();
                    newPanel = new VizContainerPanel(XML, XSL, false, myThis);	                
	            }
	        } catch(Exception ex) {}
	        
	        if(newPanel != null) {
	            // Add the rendered panel as a new tab
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

	// Simple test that loads the nineBoardTicTacToe game and visualizes
	// a randomly-played match, to demonstrate that visualization works.
	public static void main(String args[]) {
        JFrame frame = new JFrame("Visualization Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Game theGame = GameRepository.getDefaultRepository().getGame("chess");
        VisualizationPanel theVisual = new VisualizationPanel(theGame);
        frame.setPreferredSize(new Dimension(1200, 900));
        frame.getContentPane().add(theVisual);
        frame.pack();
        frame.setVisible(true);
        
        StateMachine theMachine = new CachedProverStateMachine();        
        theMachine.initialize(theGame.getRules());
        try {
            MachineState theCurrentState = theMachine.getInitialState();            
            do {
                theVisual.observe(new ServerNewGameStateEvent(theCurrentState));
                theCurrentState = theMachine.getRandomNextState(theCurrentState);
                Thread.sleep(2750);
                System.out.println("State: " + theCurrentState);
            } while(!theMachine.isTerminal(theCurrentState));
            theVisual.observe(new ServerNewGameStateEvent(theCurrentState));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}