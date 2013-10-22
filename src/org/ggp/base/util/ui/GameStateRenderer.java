package org.ggp.base.util.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.Graphics2DRenderer;
import org.xml.sax.InputSource;

/**
 * GameStateRenderer generates an image that represents the current state
 * of a match, based on the current state of the match (in XML) and an XSLT
 * that converts that XML match state into HTML. After rendering the match
 * state in HTML as a DOM, it renders that DOM into a BufferedImage which
 * can be displayed to the user.
 * 
 * TODO: This class is still pretty rough, and I suspect there's much room
 * for improvement. Furthermore, improving this class will yield immediate
 * visible benefits, in terms of better visualizations and such. For example,
 * when rendering games that don't take up the full 600x600 image, there's an
 * empty black space on the final image, which looks bad. That could be fixed.
 * 
 * @author Ethan Dreyfuss and Sam Schreiber
 */
public class GameStateRenderer {
    private static final Dimension defaultSize = new Dimension(600,600);

    public static Dimension getDefaultSize()
    {
        return defaultSize;
    }

    public static synchronized void renderImagefromGameXML(String gameXML, String XSL, BufferedImage backimage)
    {
   		Graphics2DRenderer r = new Graphics2DRenderer();

        String xhtml = getXHTMLfromGameXML(gameXML, XSL);
        xhtml = xhtml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
        
        xhtml = xhtml.replace("<body>", "<body><table width=\"560\" height=\"560\"><tr><td>");
        xhtml = xhtml.replace("</body>", "</td></tr></table></body>");
        
        InputSource is = new InputSource(new BufferedReader(new StringReader(xhtml)));
        Document dom = XMLResource.load(is).getDocument();

        r.setDocument(dom, "http://www.ggp.org/");
        final Graphics2D g2 = backimage.createGraphics();
        r.layout(g2, defaultSize);
        r.render(g2);
    }

    private static String getXHTMLfromGameXML(String gameXML, String XSL) {
        XSL = XSL.replace("<!DOCTYPE stylesheet [<!ENTITY ROOT \"http://games.ggp.org/base\">]>", "");
        XSL = XSL.replace("&ROOT;", "http://games.ggp.org/base").trim();
        
        IOString game = new IOString(gameXML);
        IOString xslIOString = new IOString(XSL);
        IOString content = new IOString("");
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslIOString.getInputStream()));
            transformer.setParameter("width", defaultSize.getWidth()-40);
            transformer.setParameter("height", defaultSize.getHeight()-40);
            transformer.transform(new StreamSource(game.getInputStream()),
                    new StreamResult(content.getOutputStream()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        Tidy tidy = new Tidy();
        tidy.setXHTML(true);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
        tidy.setDropEmptyParas(false);

        IOString tidied = new IOString("");
        tidy.parse(content.getInputStream(), tidied.getOutputStream());        
        return tidied.getString();
    }

    //========IOstring code========
    private static class IOString
    {
        private StringBuffer buf;
        public IOString(String s) {
            buf = new StringBuffer(s);
        }
        public String getString() {
            return buf.toString();
        }		

        public InputStream getInputStream() {
            return new IOString.IOStringInputStream();
        }
        public OutputStream getOutputStream() {
            return new IOString.IOStringOutputStream();
        }

        class IOStringInputStream extends java.io.InputStream {
            private int position = 0;
            public int read() throws java.io.IOException
            {
                if (position < buf.length()) {
                    return buf.charAt(position++);
                } else {
                    return -1;
                }
            }
        }
        class IOStringOutputStream extends java.io.OutputStream {
            public void write(int character) throws java.io.IOException {
                buf.append((char)character);
            }
        }
    }
}