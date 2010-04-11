package apps.kiosk.templates;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.List;

import apps.kiosk.templates.StyledGameCanvas;

public abstract class StyledGameCanvas_Rectangles extends StyledGameCanvas {
    public static final long serialVersionUID = 0x1;
    
    protected abstract List<Rectangle2D> defineRectangles();
    protected abstract void renderRectangle(Graphics g, int nRect);
    protected abstract void handleClickOnRectangle(int nRect);

    protected boolean rectanglesStartAtOne() { return true; }    
    
    private List<Rectangle2D> theRectangles = defineRectangles();
    private final int getRectByClick(int x, int y) {
        int width = mostRecentG.getClipBounds().width;
        int height = mostRecentG.getClipBounds().height;

        for(int rectId = 0; rectId < theRectangles.size(); rectId++) {
            Rectangle2D r = theRectangles.get(rectId);
            double rX = r.getX()*width;
            double rY = r.getY()*height;
            double rWidth = r.getWidth()*width;
            double rHeight = r.getHeight()*height;
            if(x > rX && x < rX + rWidth && y > rY && y < rY + rHeight)
                return rectId;
        }
        return -1;
    }
    
    private Graphics mostRecentG;
    protected void drawGameOverlay(Graphics g) {
        mostRecentG = g;
        
        int width = g.getClipBounds().width;
        int height = g.getClipBounds().height;        
        
        for(int nRect = 0; nRect < theRectangles.size(); nRect++) {
            Rectangle2D r = theRectangles.get(nRect);
            Graphics rectGraphic = g.create((int)(r.getX()*width), (int)(r.getY()*height), (int)(r.getWidth()*width), (int)(r.getHeight()*height));
            if(rectanglesStartAtOne()) {
                renderRectangle(rectGraphic, nRect+1);
            } else {
                renderRectangle(rectGraphic, nRect);
            }
        }
    }
    
    public void handleClickEvent(int x, int y) {
        // TODO: Also provide coordinates within rect?
        int nRect = getRectByClick(x, y);
        if(nRect >= 0) {
            if(rectanglesStartAtOne()) {
                handleClickOnRectangle(nRect+1);
            } else {
                handleClickOnRectangle(nRect);
            }
        }
    }
}