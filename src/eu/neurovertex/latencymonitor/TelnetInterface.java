package eu.neurovertex.latencymonitor;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Observable;

/**
 * @author Neurovertex
 *         Date: 29/09/2014, 22:14
 */
public class TelnetInterface extends Observable implements Runnable, Closeable {
	private Socket socket;
	private PrintWriter input;

	public TelnetInterface() {}

	public void start() {
		try {
			socket = new Socket(InetAddress.getLoopbackAddress(), 444);
			input = new PrintWriter(socket.getOutputStream());
			Thread t = new Thread(this, "TelnetThread");
			t.setDaemon(true);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void inputLine(String line) {
		if (socket == null)
			throw new IllegalStateException();
		setChanged();
		notifyObservers(line);
		if (socket.isConnected()) {
			input.println(line);
			input.flush();
		} else
			System.err.println("Socket isn't connected");
	}

	@Override
	public void run() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			boolean first = true;
			String line;
			while ((line = in.readLine()) != null) {
				setChanged();
				notifyObservers(line);
				if (first) {
					first = false;
					setChanged();
					notifyObservers("Successfully connected to OpenVPN's tenlet interface");
					inputLine("log on");
					inputLine("echo on");
				}
			}
		} catch (SocketException ignore) {}
		catch (IOException e) {
			e.printStackTrace();
		}
		setChanged();
		notifyObservers("Closing telnet listen thread");
	}

	@Override
	public void close() {
		inputLine("exit");
		try {
			socket.close();
		} catch (IOException ignore) {}
	}
}
