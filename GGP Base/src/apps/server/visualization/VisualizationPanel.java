package apps.server.visualization;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import server.event.ServerNewGameStateEvent;
import util.observer.Event;
import util.observer.Observer;
import util.statemachine.implementation.prover.ProverMachineState;

@SuppressWarnings("serial")
public final class VisualizationPanel extends JPanel implements Observer
{
	private final String gameName;
	private JTabbedPane tabs = new JTabbedPane();

	public VisualizationPanel(String gameName)
	{		
		this.gameName = gameName;
		this.add(tabs);
	}

	private int stepCount = 1;
	public void observe(Event event)
	{
	    if (event instanceof ServerNewGameStateEvent)
		{
	        ProverMachineState s = ((ServerNewGameStateEvent)event).getState();
	        RenderThread rt = new RenderThread(gameName, s, this, stepCount++);
	        rt.start();
		}
	}
		
	public synchronized boolean addVizPanel(JPanel newPanel, Integer stepNum)
	{
		boolean atEnd = (tabs.getSelectedIndex() == tabs.getTabCount()-1);
		try {
			for(int i = tabs.getTabCount(); i < stepNum; i++)
				tabs.add(new Integer(i+1).toString(), new JPanel());
			tabs.setComponentAt(stepNum-1, newPanel);
			tabs.setTitleAt(stepNum-1, stepNum.toString());
			
			if(atEnd) {				
				tabs.setSelectedIndex(tabs.getTabCount()-1);
			}
		} catch(Exception ex) {
			System.err.println("Adding rendered visualization panel failed for: "+gameName);
		}

		return true;
	}
}
