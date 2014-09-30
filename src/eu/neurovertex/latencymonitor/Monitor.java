package eu.neurovertex.latencymonitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Neurovertex
 *         Date: 27/05/2014, 16:12
 */
public class Monitor extends Observable implements Runnable {
	private final DatagramSocket socket;
	private final InetAddress addr;
	private final int delay;
	private final LatencyRingBuffer buffer;
	private final int port;
	private long startTime;
	private Thread thread, notifyThread;
	private volatile int id = 0;
	private volatile boolean stop = false;
	private long lastUpdate = System.currentTimeMillis();

	public Monitor(InetAddress addr, int port, int delay) throws IOException {
		this.delay = delay;
		this.addr = addr;
		this.port = port;
		socket = new DatagramSocket();
		System.out.println("Receive buffer : "+ socket.getReceiveBufferSize());
		buffer = new LatencyRingBuffer(1024);
	}

	public Monitor(byte[] addr, int port, int delay) throws IOException {
		this(InetAddress.getByAddress(addr), port, delay);
	}

	public Monitor(String hostname, int port, int delay) throws IOException {
		this(InetAddress.getByName(hostname), port, delay);
	}

	public long getPingTime(int id) {
		return startTime + id * delay;
	}

	public long getLatency(int id) {
		return System.currentTimeMillis() - getPingTime(id);
	}

	public synchronized void start() {
		thread = new Thread(this);
		thread.setDaemon(true);
		notifyThread = new Thread(new NotifyThread());
		notifyThread.setDaemon(true);
		thread.start();
		notifyThread.start();
		new Timer(true).scheduleAtFixedRate(new PingThread(), delay, delay);
	}

	public synchronized void stopMonitor() {
		thread.interrupt();
		notifyThread.interrupt();
	}

	@Override
	public void run() {
		try {
			ByteBuffer buff = ByteBuffer.allocate(256);
			DatagramPacket packet = new DatagramPacket(buff.array(), 256);
			while (!stop) {
				buff.rewind();
				socket.receive(packet);
				int i = buff.getInt(0);
				long lat = getLatency(i);
				//System.out.printf("Received:%d. Latency : %d%n", i, lat);
				buffer.set(i, lat);
				setChanged();
				synchronized (this) {
					notifyAll();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public LatencyRingBuffer getBuffer() {
		return buffer;
	}

	@Override
	public synchronized void notifyObservers() {
		if (System.currentTimeMillis() - lastUpdate > 16) { // as to avoid spamming repaint()
			setChanged();
			super.notifyObservers();
			lastUpdate = System.currentTimeMillis();
		}
	}

	private class NotifyThread implements Runnable {
		@Override
		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					synchronized (Monitor.this) {
						wait();
					}
					notifyObservers();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class PingThread extends TimerTask {
		@Override
		public void run() {
			ByteBuffer buff = ByteBuffer.allocate(4).putInt(0);
			DatagramPacket packet = new DatagramPacket(buff.array(), 4, addr, port);
			long time = System.currentTimeMillis();
			try {
				startTime = System.currentTimeMillis();
				while (!stop) {
					buff.rewind();
					buff.putInt(id);
					buffer.add();
					//System.out.printf("Sent:\t%d, %d : %d / %d%n", id, System.currentTimeMillis(), getPingTime(id), getLatency(id));
					id++;
					socket.send(packet);
					time += delay;
					notifyObservers();
					Thread.sleep(time - System.currentTimeMillis());
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
