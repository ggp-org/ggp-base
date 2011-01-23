package apps.kiosk.games;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import apps.kiosk.templates.CommonGraphics;
import apps.kiosk.templates.StyledGameCanvas_FancyRectangles;

public class QyshinsuCanvas extends StyledGameCanvas_FancyRectangles {
    public static final long serialVersionUID = 0x1;

    public String getGameName() { return "Qyshinsu"; }
    protected String getGameKey() { return "qyshinsu"; }
    protected String getGameXSL() { return "qyshinsu"; }
    
    @Override
    protected void renderMoveSelectionForRectangle(Graphics g, int nRectangle, String theMove) {
        if(isSelectedRectangle(nRectangle)) {
            String[] moveParts = theMove.split(" ");
            if(moveParts[1].equals("add")) {
                int nToAdd = Integer.parseInt(moveParts[3]);
                g.setColor(Color.GREEN);
                CommonGraphics.fillWithString(g, "" + nToAdd, 1.2);
            } else {
                g.setColor(Color.GREEN);
                CommonGraphics.fillWithString(g, "x", 1.2);
            }
        }
    }   
    
    @Override
    protected Set<String> getLegalMovesForRectangle(int nRectangle) {
        Set<String> theMoves = gameStateHasLegalMovesMatching("\\( add " + nRectangle + " (.*) \\)");
        theMoves.addAll(gameStateHasLegalMovesMatching("\\( remove " + nRectangle + " (.*) \\)"));
        return theMoves;
    }
    
    @Override
    protected Set<String> getFactsAboutRectangle(int nRectangle) {
        return getLegalMovesForRectangle(nRectangle);
    }    
    
    @Override
    protected void renderRectangleContent(Graphics g, Set<String> theFacts) {
        if(theFacts.size() > 0) {
            int width = g.getClipBounds().width;
            int height = g.getClipBounds().height;                    
            
            g.setColor(Color.GREEN);
            g.drawOval(0, 0, width, height);
        }
    }
    
    @Override
    protected List<Rectangle2D> defineRectangles() {
        List<Point2D> thePiecePoints = new ArrayList<Point2D>();
        thePiecePoints.add(new Point2D.Double(484.0/750.0, 262.0/727.0));
        thePiecePoints.add(new Point2D.Double(531.0/750.0, 323.0/727.0));
        thePiecePoints.add(new Point2D.Double(538.0/750.0, 399.0/727.0));
        thePiecePoints.add(new Point2D.Double(503.0/750.0, 469.0/727.0));
        thePiecePoints.add(new Point2D.Double(443.0/750.0, 512.0/727.0));
        thePiecePoints.add(new Point2D.Double(361.0/750.0, 518.0/727.0));
        thePiecePoints.add(new Point2D.Double(290.0/750.0, 484.0/727.0));
        thePiecePoints.add(new Point2D.Double(248.0/750.0, 426.0/727.0));
        thePiecePoints.add(new Point2D.Double(241.0/750.0, 350.0/727.0));
        thePiecePoints.add(new Point2D.Double(276.0/750.0, 279.0/727.0));
        thePiecePoints.add(new Point2D.Double(336.0/750.0, 237.0/727.0));
        thePiecePoints.add(new Point2D.Double(416.0/750.0, 232.0/727.0));
        
        double rectWidth = 50.0/750.0;
        double rectHeight = 50.0/727.0;
        List<Rectangle2D> theRectangles = new ArrayList<Rectangle2D>();
        for(Point2D thePoint : thePiecePoints) {
            theRectangles.add(new Rectangle2D.Double(thePoint.getX()-2*rectWidth/3, thePoint.getY()-2*rectHeight/3, rectWidth, rectHeight));
        }
        return theRectangles;
    }
}