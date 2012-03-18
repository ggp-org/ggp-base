package apps.kiosk.templates;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import util.statemachine.MachineState;
import util.ui.GameStateRenderer;

import apps.kiosk.GameCanvas;

public abstract class StyledGameCanvas extends GameCanvas {
    public static final long serialVersionUID = 0x1;

    protected abstract String getGameXSL();
    protected abstract void drawGameOverlay(Graphics g);
    
    /*
     * TODO: Fix this to work with web/local visualizations.
     * Right now it's completely broken.
    private String xslCache = "";
    protected final String getXSL() {
        if(!xslCache.isEmpty()) return xslCache;
        xslCache = GameStateRenderPanel.getXSLfromFile(getGameXSL());
        return xslCache;
    }
    */
    
    private Image renderedState;
    protected final Image renderedGameState() {
        if(renderedState != null)
            return renderedState;
        
        synchronized(this) {
            // String XML = gameState.toXML();
            Dimension d = GameStateRenderer.getDefaultSize();
            BufferedImage backimage = this.getGraphicsConfiguration().createCompatibleImage(d.width,d.height);        
            try { Thread.sleep(200); } catch(Exception e) { e.printStackTrace(); }
            //GameStateRenderPanel.renderImagefromGameXML(XML, XSL, true, backimage);
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(backimage, "png", bos);
                byte[] compressed = bos.toByteArray();
                
                BufferedImage img2;
                img2 = ImageIO.read(new ByteArrayInputStream(compressed));
                renderedState = img2;
            } catch (Exception ex) {ex.printStackTrace();}
        }
        return renderedState;
    }
    
    // When the game state changes, clear our cache of the rendered game state.
    public void updateGameState(MachineState gameState) {
        synchronized(this) {
            super.updateGameState(gameState);
            renderedState = null;
        }
        repaint();
    }        
    
    protected final void paintGame(Graphics g) {
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;
        
        g.setColor(this.getBackground());
        g.fillRect(0, 0, width, height);

        if(gameState == null)
            return;
        
        g.drawImage(renderedGameState(), 0, 0, width, height, null);
        drawGameOverlay(g);
    }        
}