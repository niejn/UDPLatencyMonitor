package eu.neurovertex.latencymonitor.gui;

import eu.neurovertex.latencymonitor.LatencyRingBuffer;
import eu.neurovertex.latencymonitor.Monitor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Neurovertex
 *         Date: 29/09/2014, 16:48
 */
public class LatencyDisplayPanel extends JPanel implements Observer {
	private final LatencyRingBuffer ringBuffer;
	private BufferedImage image, background;
	private long[] buff;
	private GUI gui;

	public LatencyDisplayPanel(Monitor m) {
		this(m, m.getBuffer());
	}

	public LatencyDisplayPanel(Observable obs, LatencyRingBuffer ringBuffer) { // For debug purposes
		setMinimumSize(new Dimension(500, 200));
		setPreferredSize(new Dimension(500, 300));
		setSize(500, 150);
		setMaximumSize(new Dimension(650, 500));
		image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, 0);
		setBackground(Color.BLACK);
		this.ringBuffer = ringBuffer;
		obs.addObserver(this);
	}

	private BufferedImage generateBackground() {
		BufferedImage img = new BufferedImage(getWidth(), (getHeight() > 0) ? getHeight() : 1, BufferedImage.TYPE_INT_RGB);
		float h = img.getHeight();
		int w = getWidth();
		Graphics2D g = img.createGraphics();
		for (int y = 0; y < h; y++) {
			float val = 1f - (2f+(1f-y/h)*2f) / 3f;
			Color c = new Color(Color.HSBtoRGB(Math.max(val, 0f), 1.0f, 0.5f));
			g.setColor(c);
			g.drawLine(0, y, w - 1, y);
		}
		return img;
	}

	public void setGui(GUI gui) {
		this.gui = gui;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		synchronized (this) {
			g.drawImage(image, 0, 0, null);
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		int w = getWidth(), h = getHeight(), n, lastId, offset, buffOffset;
		synchronized (ringBuffer) {
			buff = ringBuffer.getBuffer(buff);
			lastId = ringBuffer.size();
		}
		n = Math.min(lastId, buff.length);
		offset = Math.max(w - n, 0);
		buffOffset = (n > w) ? n - w : 0;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		if (background == null || w != background.getWidth() || h != background.getHeight())
			background = generateBackground();
		Graphics2D g = img.createGraphics();
		g.drawImage(background, offset, 0, null);
		Color bg = Color.black, noReply = Color.BLUE;
		long[] panelBuffer = new long[w];
		for (int i = 0; i + offset < w && i < n; i++) {
			int x = i + offset;
			if (buff[i + buffOffset] <= 0 && i < n - 1) {
				g.setColor(noReply);
				g.drawLine(x, 0, x, h - 1);
				panelBuffer[x] = -1;
			} else {
				panelBuffer[x] = buff[i + buffOffset];
				g.setColor(bg);
				float val = 1f - Math.min((float) (Math.sqrt(buff[i + buffOffset]) / 70), 1f); // 70 ~ sqrt(5000)
				g.drawLine(x, 0, x, (int) (val * h - 1));
			}
		}
		synchronized (this) {
			image.flush();
			image = img;
		}
		gui.update(o, panelBuffer);
	}

	public LatencyRingBuffer getRingBuffer() {
		return ringBuffer;
	}
}
