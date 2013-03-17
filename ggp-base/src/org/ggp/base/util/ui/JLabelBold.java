package org.ggp.base.util.ui;

import java.awt.Font;
import javax.swing.JLabel;

public class JLabelBold extends JLabel {
	private static final long serialVersionUID = 1L;
	public JLabelBold(String text) {			
		super(text);
		setFont(new Font(getFont().getFamily(), Font.BOLD, getFont().getSize()+2));
	}
}