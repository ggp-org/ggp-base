package org.ggp.base.apps.validator;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.ggp.base.util.ui.NativeUI;



import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

@SuppressWarnings("serial")
public class QueryPanel extends JPanel
{
   JTextArea jtextarea1 = new JTextArea();
   JTextArea jtextarea2 = new JTextArea();
   JButton jbutton1 = new JButton();
   JTabbedPane jtabbedpane1 = new JTabbedPane();

   /**
    * Default constructor
    */
   public QueryPanel()
   {       
       initializePanel();
   }

   /**
    * Main method for panel
    */
   public static void main(String[] args)
   {
       NativeUI.setNativeUI();
       JFrame frame = new JFrame();
       frame.setSize(600, 400);
       frame.setLocation(100, 100);
       frame.getContentPane().add(new QueryPanel());
       frame.setVisible(true);
       
       frame.addWindowListener( new WindowAdapter() {
           public void windowClosing( WindowEvent evt ) {
               System.exit(0);
           }
       });
   }

   /**
    * Adds fill components to empty cells in the first row and first column of the grid.
    * This ensures that the grid spacing will be the same as shown in the designer.
    * @param cols an array of column indices in the first row where fill components should be added.
    * @param rows an array of row indices in the first column where fill components should be added.
    */
   void addFillComponents( Container panel, int[] cols, int[] rows )
   {
       Dimension filler = new Dimension(10,10);

       boolean filled_cell_11 = false;
       CellConstraints cc = new CellConstraints();
       if ( cols.length > 0 && rows.length > 0 ) {
           if ( cols[0] == 1 && rows[0] == 1 ) {
               /** add a rigid area  */
               panel.add( Box.createRigidArea( filler ), cc.xy(1,1) );
               filled_cell_11 = true;
           }
       }
       
       for( int index = 0; index < cols.length; index++ ) {
           if ( cols[index] == 1 && filled_cell_11 ) {
               continue;
           }
           panel.add( Box.createRigidArea( filler ), cc.xy(cols[index],1) );
       }
       
       for( int index = 0; index < rows.length; index++ ) {
           if ( rows[index] == 1 && filled_cell_11 ) {
               continue;
           }
           panel.add( Box.createRigidArea( filler ), cc.xy(1,rows[index]) );
       }
   }

   /**
    * Helper method to load an image file from the CLASSPATH
    * @param imageName the package and name of the file to load relative to the CLASSPATH
    * @return an ImageIcon instance with the specified image file
    * @throws IllegalArgumentException if the image resource cannot be loaded.
    */
   public ImageIcon loadImage( String imageName )
   {
       try {
           ClassLoader classloader = getClass().getClassLoader();
           java.net.URL url = classloader.getResource( imageName );
           if (url != null) {
               ImageIcon icon = new ImageIcon( url );
               return icon;
           }
       } catch( Exception e ) {
           e.printStackTrace();
       }
       throw new IllegalArgumentException("Unable to load image: " + imageName);
   }

   /**
    * Method for recalculating the component orientation for 
    * right-to-left Locales.
    * @param orientation the component orientation to be applied
    */
   public void applyComponentOrientation( ComponentOrientation orientation )
   {
      // Not yet implemented...
      // I18NUtils.applyComponentOrientation(this, orientation);
      super.applyComponentOrientation(orientation);
   }

   public JPanel createPanel()
   {
       JPanel jpanel1 = new JPanel();
       FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,LEFT:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,TOP:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:MIN(20DLU;DEFAULT):NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
       CellConstraints cc = new CellConstraints();
       jpanel1.setLayout(formlayout1);
       
       JScrollPane jscrollpane1 = new JScrollPane();
       jscrollpane1.setViewportView(jtextarea1);
       jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
       jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       jpanel1.add(jscrollpane1,cc.xywh(9,3,11,12));
       
       JLabel jlabel1 = new JLabel();
       jlabel1.setText("Enter Query");
       jpanel1.add(jlabel1,cc.xy(9,16));
       
       JScrollPane jscrollpane2 = new JScrollPane();
       jscrollpane2.setViewportView(jtextarea2);
       jscrollpane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
       jscrollpane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       jpanel1.add(jscrollpane2,new CellConstraints(9,17,1,1,CellConstraints.FILL,CellConstraints.DEFAULT));

       jbutton1.setActionCommand("Execute");
       jbutton1.setText("Execute");
       jpanel1.add(jbutton1,cc.xy(16,17));

       jpanel1.add(jtabbedpane1,cc.xywh(3,3,5,15));

       addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 });
       return jpanel1;
   }

   /**
    * Initializer
    */
   protected void initializePanel()
   {
       setLayout(new BorderLayout());
       add(createPanel(), BorderLayout.CENTER);
   }
}