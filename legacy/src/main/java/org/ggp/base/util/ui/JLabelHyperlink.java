package org.ggp.base.util.ui;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.JLabel;

public class JLabelHyperlink extends JLabel implements MouseListener {
	private static final long serialVersionUID = 1L;
	private final String url;
	public JLabelHyperlink(String text, String url) {
		super(text);
		this.url = url;
		addMouseListener(this);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}
	@Override
	public void mouseClicked(MouseEvent arg0) {
		try {
			java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
		;
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
		;
	}
	@Override
	public void mousePressed(MouseEvent arg0) {
		;
	}
	@Override
	public void mouseReleased(MouseEvent arg0) {
		;
	}
}