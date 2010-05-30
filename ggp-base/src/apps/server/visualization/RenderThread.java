package apps.server.visualization;

import javax.swing.JPanel;

import util.statemachine.implementation.prover.ProverMachineState;
import util.xhtml.GameStateRenderPanel;

public class RenderThread extends Thread {	
	private final String gameName;
	private final ProverMachineState s;
	private final VisualizationPanel parent;
	private final int stepNum;
	
	public RenderThread(String gameName, ProverMachineState s, VisualizationPanel parent, int stepNum) {
		this.gameName = gameName;
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
			String XSL = GameStateRenderPanel.getXSLfromFile(gameName, 1); //1 because machinestate XMLs only ever have 1 state
			newPanel = new VizContainerPanel(XML, XSL, parent);
		} catch(Exception ex) {}
		
		if(newPanel != null)
			parent.addVizPanel(newPanel, stepNum);
	}
}