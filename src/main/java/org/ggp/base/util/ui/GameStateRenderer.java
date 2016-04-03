package org.ggp.base.util.ui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nu.validator.htmlparser.dom.HtmlDocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

    /* Note: NaiveUserAgent is not thread safe, so whenever the architecture
     * of this class is modified in a way that enables concurrent rendering,
     * this must be changed to another UserAgentCallback implementation that
     * uses a thread safe image cache */
    private static NaiveUserAgent userAgent = new NaiveUserAgent(128);

    public static Dimension getDefaultSize()
    {
        return defaultSize;
    }

    public static synchronized void renderImagefromGameXML(String gameXML, String XSL, BufferedImage backimage)
    {

        String xhtml = getXHTMLfromGameXML(gameXML, XSL);
        InputSource is = new InputSource(new BufferedReader(new StringReader(xhtml)));
        Document dom;
        try {
            dom = new HtmlDocumentBuilder().parse(is);

            // Many existing visualization stylesheets have style elements
            // deep within html body content, where compliant renderers interpret
            // them as text, and not as styles. So we have to pull them out.
            NodeList styles = dom.getElementsByTagName("style");
            Node head = dom.getElementsByTagName("head").item(0);
            for (int i = 0; i < styles.getLength(); i += 1) {
                Node parent = styles.item(i).getParentNode();
                if (!parent.equals(head) && parent.getNamespaceURI().contains("html")) {
                    head.appendChild(styles.item(i));
                }
            }

            Node style = dom.createElement("style");
            String bodyStyle = String.format("body { width: %dpx; height: %dpx; overflow:hidden; margin:auto;}",
                    defaultSize.width, defaultSize.height);
            style.appendChild(dom.createTextNode(bodyStyle));
            head.appendChild(style);
        } catch (SAXException | IOException ex) {
            xhtml = "<html><head><title>Error</title></head><body><h1>Error parsing visualization</h1><pre id='pre'></pre></body></html>";
            dom = XMLResource.load(new StringReader(xhtml)).getDocument();
            dom.getElementById("pre").appendChild(dom.createTextNode(ex.toString()));
            ex.printStackTrace();
        }

        Java2DRenderer r = new Java2DRenderer(dom, backimage.getWidth(), backimage.getHeight());
        r.getSharedContext().setUserAgentCallback(userAgent);

        ChainingReplacedElementFactory chainingReplacedElementFactory = new ChainingReplacedElementFactory();
        chainingReplacedElementFactory.addReplacedElementFactory(r.getSharedContext().getReplacedElementFactory());
        chainingReplacedElementFactory.addReplacedElementFactory(new SVGReplacedElementFactory());
        r.getSharedContext().setReplacedElementFactory(chainingReplacedElementFactory);

        backimage.setData(r.getImage().getData());
    }

    public static synchronized void shrinkCache() {
        userAgent.shrinkImageCache();
    }

    private static String getXHTMLfromGameXML(String gameXML, String XSL) {
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

        return content.getString();
    }

    //========IOstring code========
    private static class IOString
    {
        private StringBuilder buf;
        public IOString(String s) {
            buf = new StringBuilder(s);
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
            @Override
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
            @Override
            public void write(int character) throws java.io.IOException {
                buf.append((char)character);
            }
        }
    }
}