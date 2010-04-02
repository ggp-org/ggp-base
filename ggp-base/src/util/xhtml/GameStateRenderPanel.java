package util.xhtml;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
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
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
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
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xhtmlrenderer.util.XRLog;
import org.xml.sax.InputSource;

import util.configuration.ProjectConfiguration;
import util.files.FileUtils;


/**
 * A mess of code which is responsible for generating a graphical rendering of a game
 * @author Ethan
 *
 */
@SuppressWarnings("serial")
public class GameStateRenderPanel extends JPanel {
	
	private static final String baseURL = "http://visionary.stanford.edu:4000";
	private static final Dimension defaultSize = new Dimension(600,600);
	
	public static Dimension getDefaultSize()
	{
		return defaultSize;
	}
	
	public static JPanel getPanelfromGameXML(String gameXML, String XSL)
	{
		XHTMLPanel panel = new XHTMLPanel();
		panel.setPreferredSize(defaultSize);
		//setupUserAgentCallback(panel);
		
		String XHTML = getXHTMLfromGameXML(gameXML, XSL);
		setPanelToDisplayGameXHTML(panel, XHTML);
		panel.getDocument();
			
		return panel;
	}
	
	public static void renderImagefromGameXML(String gameXML, String XSL, BufferedImage backimage)
	{		
		Graphics2DRenderer r = new Graphics2DRenderer();
		r.getSharedContext().setUserAgentCallback(getUAC());
		
		String xhtml = getXHTMLfromGameXML(gameXML, XSL);
		InputSource is = new InputSource(new BufferedReader(new StringReader(xhtml)));
        Document dom = XMLResource.load(is).getDocument();
        
		r.setDocument(dom, baseURL);
		final Graphics2D g2 = backimage.createGraphics();
		r.layout(g2, defaultSize);
		r.render(g2);
	}

	public static String getXHTMLfromGameXML(String gameXML, Integer turnToShow) {
		String XSLstring = findXSLT(gameXML);
		String XSL = getXSLfromFile(XSLstring, turnToShow);
		return getXHTMLfromGameXML(gameXML, XSL);		
	}
	
	public static String getXHTMLfromGameXML(String gameXML, String XSL) {
		IOString game = new IOString();
		game.setString(gameXML);		
		IOString xslIOString = new IOString();
		xslIOString.setString(XSL);
		IOString content = new IOString();
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer(new StreamSource(xslIOString.getInputStream()));
			
			transformer.transform(	new StreamSource(game.getInputStream()), 
									new StreamResult(content.getOutputStream()));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		String tcontent = content.getString();
		content.setString(tcontent);
		Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		tidy.setShowWarnings(false);
		tidy.setQuiet(true);
		tidy.setDropEmptyParas(false);
		IOString tidied = new IOString();
		tidy.parse(content.getInputStream(), tidied.getOutputStream());
		tcontent = tidied.getString();
		return tcontent;
	}
	
	public static String getXSLfromFile(String XSLfileName, Integer turnToShow)
	{
		File xslFile = new File(ProjectConfiguration.gameStylesheetsDirectory, XSLfileName);
		String XSL = FileUtils.readFileAsString(xslFile);
		String CustomXSL = getCustomXSL(XSL);
		File templateFile = new File(new File(new File("src", "util"), "xhtml"), "template.xsl");
		String template = FileUtils.readFileAsString(templateFile);
		XSL = template.replace("###GAME_SPECIFIC_STUFF_HERE###", CustomXSL);
		XSL = XSL.replace("###STATE_NUM_HERE###", turnToShow.toString());
		return XSL;
	}
	
	private static String findXSLT(String gameXML)
	{		
		final String toFind = "<?xml-stylesheet type='text/xsl' href='/docserver/gameserver/stylesheets/";
		int start = gameXML.indexOf(toFind)+toFind.length();
		int end = gameXML.indexOf('\'', start);
		return gameXML.substring(start, end);
	}
	
	private static String getCustomXSL(String XSL)
	{
		final String toFind = "<!-- Game specific stuff goes here -->";
		int start = XSL.indexOf(toFind)+toFind.length();
		int end = XSL.indexOf(toFind, start);
		return XSL.substring(start, end);
	}
	
	private void setPanelToDisplayGameXML(XHTMLPanel panel, String gameXML, Integer turnToShow)
	{
		String XHTML = getXHTMLfromGameXML(gameXML, turnToShow);
	    try {
			panel.setDocumentFromString(XHTML, baseURL, new XhtmlNamespaceHandler());			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void setPanelToDisplayGameXHTML(XHTMLPanel panel, String XHTML)
	{
		try {
			panel.setDocumentFromString(XHTML, baseURL, getHandler());			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static XhtmlNamespaceHandler the_handler = null;
	private static XhtmlNamespaceHandler getHandler()
	{
		if(the_handler==null)
			the_handler = new XhtmlNamespaceHandler();
		return the_handler;
	}

	//Sharing UACs would probably help reduce resource usage, but I'm not sure about thread-safety of UAC (it seemed not to be)
//	private static UserAgentCallback uac = null;
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
                //TODO: check that cached image is still valid
                if (ir == null) {
                	InputStream is = null;
                	
                	String[] chunks = uri.split("/");
                	String filename = chunks[chunks.length-1];
                	File localImg = new File("games", "images");
                	for(int i=chunks.length-1; i>0; i--)
                		if(chunks[i].equals("images"))
                		{
                			for(int j=i+1; j<chunks.length-1; j++)
                				localImg = new File(localImg, chunks[j]);                				
                			break;
                		}
                	localImg.mkdirs();
                	localImg = new File(localImg, filename);
                	
                	boolean presentLocally = localImg.exists();
                	if(presentLocally)
                	{
                		try {
                			is = new FileInputStream(localImg);
                		} catch(Exception ex) { ex.printStackTrace(); }
                	}
                	else
                	{
                	    is = resolveAndOpenStream(uri);
                	}
                	
                    if (is != null) {
                        try {
                            BufferedImage img = ImageIO.read(is);
                            if(!presentLocally)
                            {
                            	ImageIO.write(img, "png", localImg);                       	
                            }
                            if (img == null) {
                                throw new IOException("ImageIO.read() returned null");
                            }
                            ir = createImageResource(uri, img);
                            _imageCache.put(uri, ir);
                        } catch (FileNotFoundException e) {
                            XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
                        } catch (IOException e) {
                            XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                        } finally {
                            try {
                                is.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                }
                if (ir == null) {
                    ir = createImageResource(uri, null);
                }
                return ir;
            }
        };
    }

	
//=======Test App code=========
	private static void createAndShowGUI(GameStateRenderPanel renderPanel)
	{
		JFrame frame = new JFrame("Game State");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setPreferredSize(new Dimension(1024, 768));
		frame.getContentPane().add(renderPanel);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException
	{
		final GameStateRenderPanel me = new GameStateRenderPanel();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				createAndShowGUI(me);
			}
		});
	}
	
	File gameXMLFile = new File(new File(new File("src", "util"), "xhtml"), "sampleMath.xml");
	String gameXML = FileUtils.readFileAsString(gameXMLFile);

	int curTurn = 1;
	XHTMLPanel mypanel;
	private final JButton forward;
	private final JButton back;
	public GameStateRenderPanel()
	{
		super(new GridBagLayout());
		forward = new JButton(new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				setPanelToDisplayGameXML(mypanel, gameXML, ++curTurn);
			}});
		back = new JButton(new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				setPanelToDisplayGameXML(mypanel, gameXML, --curTurn);
			}});
		forward.setText("Forward");
		back.setText("Back");
		
		mypanel = new XHTMLPanel();
		//setupUserAgentCallback(panel);
		
		setPanelToDisplayGameXML(mypanel, gameXML, 1);
		
	    //FSScrollPane scroll = new FSScrollPane(mypanel);
	    //scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    //scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	    
	    this.add(back, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
	    this.add(forward, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));	    
		this.add(mypanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}
	
//========IOstring code========
	public static class IOString
	{
		private StringBuffer buf;
		/** Creates a new instance of IOString */
		public IOString()
		{
			buf = new StringBuffer();
		}
		public IOString(String text)
		{
			buf = new StringBuffer(text);
		}
		public InputStream getInputStream()
		{
			return new IOString.IOStringInputStream();
		}
		public OutputStream getOutputStream()
		{
			return new IOString.IOStringOutputStream();
		}
		public String getString()
		{
			return buf.toString();
		}
		public void setString(String s)
		{
			buf = new StringBuffer(s);
		}
		class IOStringInputStream extends java.io.InputStream
		{
			private int position = 0;
			public int read() throws java.io.IOException
			{
				if (position<buf.length())
				{
					return buf.charAt(position++);
				}else
				{
					return -1;
				}
			}
		}
		class IOStringOutputStream extends java.io.OutputStream
		{
			public void write(int character) throws java.io.IOException
			{
				buf.append((char)character);
			}

		}
	}

}
