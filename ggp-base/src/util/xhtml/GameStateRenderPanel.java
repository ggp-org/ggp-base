package util.xhtml;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.Graphics2DRenderer;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xml.sax.InputSource;

import util.configuration.LocalResourceLoader;
import util.files.FileUtils;

/**
 * A mess of code which is responsible for generating a graphical rendering of a game
 * @author Ethan
 *
 */
@SuppressWarnings("serial")
public class GameStateRenderPanel extends JPanel {
    private static final Dimension defaultSize = new Dimension(600,600);

    public static Dimension getDefaultSize()
    {
        return defaultSize;
    }

    public static void renderImagefromGameXML(String gameXML, String XSL, boolean isLocalVisualization, BufferedImage backimage)
    {
        Graphics2DRenderer r = new Graphics2DRenderer();
        if (isLocalVisualization) {
          r.getSharedContext().setUserAgentCallback(getUAC());
        }

        String xhtml = getXHTMLfromGameXML(gameXML, XSL);
        xhtml = xhtml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
        
        xhtml = xhtml.replace("<body>", "<body><table width=\"400\" height=\"400\"><tr><td>");
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
            //transformer.setParameter("width", defaultSize.getWidth());
            //transformer.setParameter("height", defaultSize.getHeight());
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

    // Sharing UACs would probably help reduce resource usage,
    // but I'm not sure about thread-safety of UAC (it seemed not to be).
    private static UserAgentCallback getUAC() {
        return getNewUAC();
    }
    private static UserAgentCallback getNewUAC() {
        return new NaiveUserAgent() {
            //TOdO:implement this with nio.
            @SuppressWarnings("unchecked")
            // TODO: _imageCache should be templatized properly so that warnings don't need to be suppressed
            @Override
            public ImageResource getImageResource(String uri)
            {
                ImageResource ir;
                uri = resolveURI(uri);
                ir = (ImageResource) _imageCache.get(uri);
                if (ir != null) {
                  return ir;
                }
                
                // Couldn't load image from cache: need to fetch original source.
                InputStream is = null;
                String expectedPrefix = "http://www.ggp.org/docserver/gamemaster/images/"; 
                if (!uri.startsWith(expectedPrefix)) {
                    System.err.println("Unexpected prefix for image URI: " + uri);
                    return createImageResource(uri, null);
                }
                File localImg = new File(new File("games", "images"), uri.replace(expectedPrefix, ""));

                // Ensure the image is present on the local disk.
                boolean presentLocally = localImg.exists();
                if (!presentLocally) {
                  return createImageResource(uri, null);
                }
                
                // Open a stream from the file on the local disk.
                try {
                  is = new FileInputStream(localImg);
                } catch(Exception ex) {
                  ex.printStackTrace();
                }
                if (is == null) {
                  return createImageResource(uri, null);
                }

                // Read the image from the stream.
                try {
                    BufferedImage img = ImageIO.read(is);
                    if (img == null) {
                      System.err.println("ImageIO.read() returned null");
                      throw new IOException("ImageIO.read() returned null");
                    }
                    ir = createImageResource(uri, img);
                    _imageCache.put(uri, ir);
                } catch (FileNotFoundException e) {
                    System.err.println("Can't read image file; image at URI '" + uri + "' not found");
                } catch (IOException e) {
                    System.err.println("Can't read image file; unexpected problem for URI '" + uri + "': " + e);
                } finally {
                    try {
                      is.close();
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                }
                if (ir != null) {
                    return ir;
                }
                
                // Couldn't fetch original image source; need to create stub.
                return createImageResource(uri, null);
            }
        };
    }
    
    //========XSL Loading code=====
    
    // Code to pull in XSL from local stylesheets
    public static String getXSLfromFile(String theGameKey) {        
        File templateFile = new File(new File(new File("src", "util"), "xhtml"), "template.xsl");
        String XSL = LocalResourceLoader.loadStylesheet(theGameKey);
        String template = FileUtils.readFileAsString(templateFile);
        return template.replace("###GAME_SPECIFIC_STUFF_HERE###", XSL);
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