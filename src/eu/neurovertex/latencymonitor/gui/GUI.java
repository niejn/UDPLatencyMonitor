package eu.neurovertex.latencymonitor.gui;


import eu.neurovertex.latencymonitor.Monitor;
import eu.neurovertex.latencymonitor.TelnetInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

/**
 * @author Neurovertex
 *         Date: 29/09/2014, 16:45
 */
public class GUI extends MouseAdapter implements MouseMotionListener, Observer {
	private final Optional<JScrollPane> scroll;
	private final JFrame window;
	private final LatencyDisplayPanel latency;
	private final Optional<JTextField> inputField;
	private long[] panelBuffer = new long[0];
	private Long lastPosition;

	public GUI(LatencyDisplayPanel latency, Optional<TelnetInterface> opTelnet) throws IOException {
		this.latency = latency;
		latency.setGui(this);
		latency.addMouseMotionListener(this);
		latency.addMouseListener(this);
		window = new JFrame("Latency Monitor");
		Dimension d = new Dimension((int) latency.getPreferredSize().getWidth(), 250);
		window.setMinimumSize(d);

		window.getContentPane().setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));

		if (!opTelnet.isPresent()) {
			inputField = Optional.empty();
			scroll = Optional.empty();
			window.add(latency);
			window.add(new LatencyInformationPanel());
		} else {
			TelnetInterface telnet = opTelnet.get();
			TextBufferDisplayer textArea = new TextBufferDisplayer();
			JScrollPane scroll = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			this.scroll = Optional.of(scroll);
			scroll.setPreferredSize(new Dimension(650, 100));
			scroll.setMaximumSize(new Dimension(650, 200));
			scroll.setMinimumSize(new Dimension(200, 50));

			JTextField inputField = new JTextField(50);
			this.inputField = Optional.of(inputField);
			inputField.addActionListener(e -> {
				telnet.inputLine(inputField.getText());
				inputField.setText("");
			});
			inputField.setMaximumSize(new Dimension(650, 21));

			JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
			toolBar.setPreferredSize(new Dimension(650, 30));
			toolBar.setFloatable(false);
			JButton button;
			ActionListener buttonListener = e -> {
				if (e.getActionCommand().startsWith("signal"))
					telnet.inputLine(e.getActionCommand());
				else if (e.getActionCommand().equals("show") || e.getActionCommand().equals("hide")) {
					scroll.setVisible(e.getActionCommand().equals("show"));
					inputField.setVisible(e.getActionCommand().equals("show"));
				} else if (e.getActionCommand().equals("retry")) {
					telnet.retry();
				} else {
					if (e.getActionCommand().equals("stop"))
						telnet.close();
					ProcessObservable proc = new ProcessObservable("net", e.getActionCommand(), "OpenVPNService");
					if (e.getActionCommand().equals("start"))
						new Thread(() -> {
							proc.waitProcess();
							telnet.start();
						});
					proc.addObserver(textArea);
					proc.start();
				}
			};
			toolBar.add(button = new JButton("USR1"));
			button.setActionCommand("signal SIGUSR1");
			button.addActionListener(buttonListener);

			toolBar.add(button = new JButton("HUP"));
			button.setActionCommand("signal SIGHUP");
			button.addActionListener(buttonListener);

			toolBar.add(button = new JButton("Stop"));
			button.setActionCommand("stop");
			button.addActionListener(buttonListener);

			toolBar.add(button = new JButton("Start"));
			button.setActionCommand("start");
			button.addActionListener(buttonListener);

			toolBar.add(button = new JButton("Show OVPN"));
			button.setActionCommand("show");
			button.addActionListener(buttonListener);

			toolBar.add(button = new JButton("Hide OVPN"));
			button.setActionCommand("hide");
			button.addActionListener(buttonListener);

			toolBar.add(button = new JButton("Retry OVPN"));
			button.setActionCommand("retry");
			button.addActionListener(buttonListener);

			telnet.addObserver(textArea);
			telnet.addObserver(this);

			window.add(toolBar);
			window.add(latency);
			window.add(new LatencyInformationPanel());
			window.add(scroll);
			window.add(inputField);
			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					telnet.close();
				}
			});
		}

		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		window.pack();
		window.setVisible(true);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof Monitor && arg != null && arg instanceof long[])
			panelBuffer = (long[]) arg;
		window.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		lastPosition = (e.getX() < panelBuffer.length) ? panelBuffer[e.getX()] : null;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		lastPosition = (e.getX() < panelBuffer.length) ? panelBuffer[e.getX()] : null;
	}

	@Override
	public void mouseExited(MouseEvent e) {
		lastPosition = null;
	}

	private class LatencyInformationPanel extends JPanel {
		public LatencyInformationPanel() {
			int infoHeight = 20;
			setMinimumSize(new Dimension(200, infoHeight));
			setPreferredSize(new Dimension(500, infoHeight));
			setMaximumSize(new Dimension(650, infoHeight));
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());

			g.setColor(Color.BLACK);
			StringBuilder sb = new StringBuilder();
			long sum = 0;
			int count = 0, lost = 0, last = 0;
			boolean found = false;
			for (int i = panelBuffer.length - 1; i > 0 && i > panelBuffer.length - 1 - latency.getRingBuffer().size() && count < 100; i--) {
				if (!found && panelBuffer[i] > 0) {
					found = true;
					last = panelBuffer.length - 1 - i;
				} else if (found) {
					if (panelBuffer[i] <= 0) {
						if (i < panelBuffer.length - 1)
							lost++;
					} else
						sum += panelBuffer[i];
					count++;
				}
			}

			if (count > 0)
				sb.append("Lost frames : ").append((100 * lost / count)).append("%, average latency : ").append(sum / count).append(", frames since last reply : ").append(last);
			if (lastPosition != null && lastPosition != 0)
				sb.append(", pointed : ").append((lastPosition > 0) ? String.valueOf(lastPosition) : "Frame lost");
			sb.append('.');
			g.drawString(sb.toString(), 5, getHeight() - 5);
		}
	}
}
