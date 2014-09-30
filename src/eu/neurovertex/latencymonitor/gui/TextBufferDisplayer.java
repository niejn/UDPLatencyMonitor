package eu.neurovertex.latencymonitor.gui;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Neurovertex
 *         Date: 29/09/2014, 22:43
 */
public class TextBufferDisplayer extends JTextArea implements Observer {

	public TextBufferDisplayer(int lines) {
		super("EMPTY TEXTAREA");
		setEditable(false);
		setCaretPosition(getDocument().getLength());
		((DefaultCaret)getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}


	@Override
	public void update(Observable o, Object arg) {
		//boolean scroll = (getCaretPosition() == getDocument().getLength());
		append(arg+ "\n");
		/*if (scroll)
			setCaretPosition(getDocument().getLength());*/
	}
}
