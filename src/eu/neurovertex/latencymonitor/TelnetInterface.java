package eu.neurovertex.latencymonitor;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Observable;

/**
 * @author Neurovertex
 *         Date: 29/09/2014, 22:14
 */
public class TelnetInterface extends Observable implements Runnable, Closeable {
	private Socket socket;
	private PrintWriter input;
	private boolean retry = false;

	public TelnetInterface() {
	}

	public void start() {
		Thread t = new Thread(this, "TelnetThread");
		t.setDaemon(true);
		t.start();
	}

	public void inputLine(String line) {
		if (socket == null)
			return;
		notifyObservers("> "+ line);
		if (socket != null && socket.isConnected()) {
			try {
				input.println(line);
				input.flush();
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.err.println("Socket isn't connected");
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			if (socket == null) {
				try {
					notifyObservers("Attempting to connect to OpenVPN ...");
					socket = new Socket(InetAddress.getLoopbackAddress(), 444);
					input = new PrintWriter(socket.getOutputStream());
					notifyObservers("Successfully connected to OpenVPN's tenlet interface");
					inputLine("log on");
					inputLine("echo on");
				} catch (IOException e) {
					notifyObservers("Couldn't open telnet connection");
					while (!retry)
						synchronized (this) {
							try {
								wait();
							} catch (InterruptedException ignore) {}
						}
					retry = false;
				}
			} else
				try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
					String line;
					while ((line = in.readLine()) != null) {
						notifyObservers(line);
					}
				} catch (IOException e) {
					notifyObservers("Error on the telnet thread. ");
					socket = null;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignore) {}
				}
		}
		notifyObservers("Closing telnet listen thread");
	}

	@Override
	public void notifyObservers(Object arg) {
		setChanged();
		super.notifyObservers(arg);
	}

	public boolean isConnected() {
		return socket != null && socket.isConnected();
	}

	public boolean isTrying() {
		return retry;
	}

	@Override
	public void close() {
		try {
			inputLine("exit");
			socket.close();
		} catch (Exception ignore) {
		}
	}

	public synchronized void retry() {
		this.retry = true;
		notifyAll();
	}
}
