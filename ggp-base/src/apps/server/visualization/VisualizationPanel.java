package apps.server.visualization;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import server.event.ServerNewGameStateEvent;
import util.game.Game;
import util.game.GameRepository;
import util.observer.Event;
import util.observer.Observer;
import util.statemachine.MachineState;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverMachineState;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import util.xhtml.GameStateRenderPanel;

@SuppressWarnings("serial")
public final class VisualizationPanel extends JPanel implements Observer
{
	private final Game theGame;
	private final VisualizationPanel myThis;
	private JTabbedPane tabs = new JTabbedPane();

	public VisualizationPanel(Game theGame)
	{		
		this.theGame = theGame;
		this.myThis = this;
		this.add(tabs);		
	}

	private int stepCount = 1;
	public void observe(Event event)
	{
	    if (event instanceof ServerNewGameStateEvent)
		{
	        MachineState s = ((ServerNewGameStateEvent)event).getState();
	        RenderThread rt = new RenderThread(s, stepCount++);
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
	            String XML = s.toXML();
	            String XSL = GameStateRenderPanel.getXSLfromFile(theGame.getKey());
	            
	            // TODO: Figure out a way to render visualizations using the web stylesheets.
	            //String XSL = theGame.getStylesheet();
	            
	            newPanel = new VizContainerPanel(XML, XSL, myThis);
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
        
        Game theGame = GameRepository.getDefaultRepository().getGame("nineBoardTicTacToe");
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
                theVisual.observe(new ServerNewGameStateEvent((ProverMachineState)theCurrentState));
                theCurrentState = theMachine.getRandomNextState(theCurrentState);
                Thread.sleep(750);
                System.out.println("State: " + theCurrentState);
            } while(!theMachine.isTerminal(theCurrentState));
            theVisual.observe(new ServerNewGameStateEvent((ProverMachineState)theCurrentState));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}