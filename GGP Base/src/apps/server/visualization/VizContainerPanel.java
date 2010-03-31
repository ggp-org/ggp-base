package apps.server.visualization;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import util.xhtml.GameStateRenderPanel;

@SuppressWarnings("serial")
public class VizContainerPanel extends JPanel {
	
	public final String XML;
	public final String XSL;
	public VizContainerPanel(String XML, String XSL, VisualizationPanel parent)
	{
		this.XML = XML;
		this.XSL = XSL;
		Dimension d = GameStateRenderPanel.getDefaultSize();
		this.setPreferredSize(d);
		BufferedImage backimage = parent.getGraphicsConfiguration().createCompatibleImage(d.width,d.height);
		try { Thread.sleep(1000); } catch(Exception e) { e.printStackTrace(); }
		GameStateRenderPanel.renderImagefromGameXML(XML, XSL, backimage);
		try
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(backimage, "png", bos);
			compressed = bos.toByteArray();
			imageWritten = true;
		} catch (Exception ex) {ex.printStackTrace();}
	}
	
	private byte[] compressed = null;
	private boolean imageWritten = false;
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (imageWritten) {
			try {
				BufferedImage img2;
				img2 = ImageIO.read(new ByteArrayInputStream(compressed));
				g.drawImage(img2, 0, 0, null);
			} catch (Exception ex) {ex.printStackTrace();}
       }
	}
}
