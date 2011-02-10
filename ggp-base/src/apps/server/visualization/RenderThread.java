package apps.server.visualization;

import javax.swing.JPanel;

import util.game.Game;
import util.statemachine.MachineState;
import util.xhtml.GameStateRenderPanel;

public class RenderThread extends Thread {	
	private final Game theGame;
	private final MachineState s;
	private final VisualizationPanel parent;
	private final int stepNum;
	
	public RenderThread(Game theGame, MachineState s, VisualizationPanel parent, int stepNum) {
		this.theGame = theGame;
		this.s = s;
		this.parent = parent;
		this.stepNum = stepNum;
	}
	
	@Override
	public void run()
	{
		JPanel newPanel = null;
		try {
			String XML = s.toXML();
			String XSL = GameStateRenderPanel.getXSLfromFile(theGame.getKey(), 1); //1 because machinestate XMLs only ever have 1 state
			
			// TODO: Figure out a way to render visualizations using the web stylesheets.
			//String XSL = theGame.getStylesheet();
			
			newPanel = new VizContainerPanel(XML, XSL, parent);
		} catch(Exception ex) {}
		
		if(newPanel != null)
			parent.addVizPanel(newPanel, stepNum);
	}
}